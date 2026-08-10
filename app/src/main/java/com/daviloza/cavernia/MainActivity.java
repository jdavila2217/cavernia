package com.daviloza.cavernia;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Cavernia — contenedor nativo del juego.
 * El juego vive en assets/index.html y se comunica con esta clase
 * a través del objeto JavaScript "Cavernia".
 */
public class MainActivity extends Activity implements PurchasesUpdatedListener {

    private static final String TAG = "Cavernia";

    /* ==========================================================
       IDs DE PRUEBA DE ADMOB.
       Reemplázalos por los tuyos antes de publicar en Play.
       Con estos IDs los anuncios siempre se muestran y no generan
       ingresos: sirven para probar sin que Google te suspenda.
       ========================================================== */
    private static final String ID_BANNER        = "ca-app-pub-3940256099942544/6300978111";
    private static final String ID_INTERSTICIAL  = "ca-app-pub-3940256099942544/1033173712";
    private static final String ID_RECOMPENSADO  = "ca-app-pub-3940256099942544/5224354917";

    /** ID del producto que debes crear en Play Console > Monetizar > Productos integrados. */
    private static final String PRODUCTO_SIN_ANUNCIOS = "quitar_anuncios";

    private WebView web;
    private AdView banner;
    private InterstitialAd intersticial;
    private RewardedAd recompensado;

    private BillingClient facturacion;
    private ProductDetails detalleSinAnuncios;
    private SharedPreferences prefs;
    private boolean sinAnuncios = false;

    /* ============================ ciclo de vida ============================ */

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle estado) {
        super.onCreate(estado);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefs = getSharedPreferences("cavernia", MODE_PRIVATE);
        sinAnuncios = prefs.getBoolean("sinAnuncios", false);

        /* --- WebView con el juego --- */
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);            // guarda el progreso del juego
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setWebViewClient(new WebViewClient());
        web.setBackgroundColor(0xFF16110D);
        web.setLongClickable(false);
        web.setHapticFeedbackEnabled(false);
        web.addJavascriptInterface(new Puente(), "Cavernia");

        /* --- banner abajo, oculto mientras se juega --- */
        banner = new AdView(this);
        banner.setAdUnitId(ID_BANNER);
        banner.setAdSize(AdSize.BANNER);
        banner.setVisibility(View.GONE);
        FrameLayout.LayoutParams lpBanner = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lpBanner.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;

        FrameLayout raiz = new FrameLayout(this);
        raiz.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        raiz.addView(banner, lpBanner);
        setContentView(raiz);

        /* --- AdMob --- */
        if (!sinAnuncios) {
            MobileAds.initialize(this, new OnInitializationCompleteListener() {
                @Override public void onInitializationComplete(InitializationStatus st) {
                    banner.loadAd(new AdRequest.Builder().build());
                    cargarIntersticial();
                    cargarRecompensado();
                }
            });
        }

        iniciarFacturacion();
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
        if (banner != null) banner.resume();
        consultarCompras();
    }

    @Override protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
        if (banner != null) banner.pause();
    }

    @Override protected void onDestroy() {
        if (banner != null) banner.destroy();
        if (facturacion != null) facturacion.endConnection();
        super.onDestroy();
    }

    @Override public void onWindowFocusChanged(boolean tieneFoco) {
        super.onWindowFocusChanged(tieneFoco);
        if (tieneFoco) pantallaCompleta();
    }

    private void pantallaCompleta() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onBackPressed() {
        // El botón atrás manda el juego a la pantalla de pausa, no lo cierra.
        aJs("document.getElementById('pausa').click();");
    }

    /* ============================ puente JS ============================ */

    private void aJs(final String codigo) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (web != null) web.evaluateJavascript(codigo, null);
            }
        });
    }

    private void aviso(final String texto) {
        runOnUiThread(new Runnable() {
            @Override public void run() { Toast.makeText(MainActivity.this, texto, Toast.LENGTH_SHORT).show(); }
        });
    }

    /** Métodos que el juego puede llamar como window.Cavernia.xxx() */
    private class Puente {

        @JavascriptInterface public boolean anunciosQuitados() { return sinAnuncios; }

        @JavascriptInterface public void mostrarBanner(final boolean visible) {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (banner != null) banner.setVisibility((visible && !sinAnuncios) ? View.VISIBLE : View.GONE);
                }
            });
        }

        @JavascriptInterface public void mostrarIntersticial() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (sinAnuncios || intersticial == null) { aJs("window.Puente.intersticialCerrado();"); return; }
                    intersticial.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() {
                            intersticial = null; cargarIntersticial();
                            aJs("window.Puente.intersticialCerrado();");
                        }
                        @Override public void onAdFailedToShowFullScreenContent(AdError e) {
                            intersticial = null; cargarIntersticial();
                            aJs("window.Puente.intersticialCerrado();");
                        }
                    });
                    intersticial.show(MainActivity.this);
                }
            });
        }

        @JavascriptInterface public void verVideo() {
            runOnUiThread(new Runnable() {
                @Override public void run() {
                    if (recompensado == null) {
                        aviso("El video no está listo todavía, intenta en un momento.");
                        cargarRecompensado();
                        return;
                    }
                    recompensado.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override public void onAdDismissedFullScreenContent() { recompensado = null; cargarRecompensado(); }
                        @Override public void onAdFailedToShowFullScreenContent(AdError e) { recompensado = null; cargarRecompensado(); }
                    });
                    recompensado.show(MainActivity.this, new OnUserEarnedRewardListener() {
                        @Override public void onUserEarnedReward(RewardItem premio) {
                            aJs("window.Puente.recompensa();");
                        }
                    });
                }
            });
        }

        @JavascriptInterface public void comprarSinAnuncios() {
            runOnUiThread(new Runnable() {
                @Override public void run() { lanzarCompra(); }
            });
        }
    }

    /* ============================ AdMob ============================ */

    private void cargarIntersticial() {
        if (sinAnuncios) return;
        InterstitialAd.load(this, ID_INTERSTICIAL, new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override public void onAdLoaded(InterstitialAd ad) { intersticial = ad; }
                    @Override public void onAdFailedToLoad(LoadAdError e) {
                        intersticial = null; Log.w(TAG, "intersticial: " + e.getMessage());
                    }
                });
    }

    private void cargarRecompensado() {
        if (sinAnuncios) return;
        RewardedAd.load(this, ID_RECOMPENSADO, new AdRequest.Builder().build(),
                new RewardedAdLoadCallback() {
                    @Override public void onAdLoaded(RewardedAd ad) { recompensado = ad; }
                    @Override public void onAdFailedToLoad(LoadAdError e) {
                        recompensado = null; Log.w(TAG, "recompensado: " + e.getMessage());
                    }
                });
    }

    /* ============================ compras en Play ============================ */

    private void iniciarFacturacion() {
        facturacion = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        facturacion.startConnection(new BillingClientStateListener() {
            @Override public void onBillingSetupFinished(BillingResult r) {
                if (r.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    consultarProducto();
                    consultarCompras();
                }
            }
            @Override public void onBillingServiceDisconnected() { }
        });
    }

    private void consultarProducto() {
        List<QueryProductDetailsParams.Product> lista = new ArrayList<>();
        lista.add(QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCTO_SIN_ANUNCIOS)
                .setProductType(BillingClient.ProductType.INAPP)
                .build());
        facturacion.queryProductDetailsAsync(
                QueryProductDetailsParams.newBuilder().setProductList(lista).build(),
                new ProductDetailsResponseListener() {
                    @Override public void onProductDetailsResponse(BillingResult r, List<ProductDetails> detalles) {
                        if (detalles != null && !detalles.isEmpty()) detalleSinAnuncios = detalles.get(0);
                    }
                });
    }

    private void lanzarCompra() {
        if (facturacion == null || detalleSinAnuncios == null) {
            aviso("La tienda no está disponible ahora mismo.");
            return;
        }
        BillingFlowParams.ProductDetailsParams p = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(detalleSinAnuncios)
                .build();
        facturacion.launchBillingFlow(this, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Arrays.asList(p))
                .build());
    }

    private void consultarCompras() {
        if (facturacion == null || !facturacion.isReady()) return;
        facturacion.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
                new PurchasesResponseListener() {
                    @Override public void onQueryPurchasesResponse(BillingResult r, List<Purchase> compras) {
                        if (compras == null) return;
                        for (Purchase c : compras) procesarCompra(c);
                    }
                });
    }

    @Override public void onPurchasesUpdated(BillingResult r, List<Purchase> compras) {
        if (r.getResponseCode() == BillingClient.BillingResponseCode.OK && compras != null) {
            for (Purchase c : compras) procesarCompra(c);
        } else if (r.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            aviso("Compra cancelada.");
        }
    }

    private void procesarCompra(Purchase compra) {
        if (compra.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;
        if (!compra.getProducts().contains(PRODUCTO_SIN_ANUNCIOS)) return;

        if (!compra.isAcknowledged()) {
            facturacion.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(compra.getPurchaseToken()).build(),
                    new AcknowledgePurchaseResponseListener() {
                        @Override public void onAcknowledgePurchaseResponse(BillingResult r) { }
                    });
        }
        activarSinAnuncios();
    }

    private void activarSinAnuncios() {
        if (sinAnuncios) return;
        sinAnuncios = true;
        prefs.edit().putBoolean("sinAnuncios", true).apply();
        intersticial = null;
        recompensado = null;
        runOnUiThread(new Runnable() {
            @Override public void run() { if (banner != null) banner.setVisibility(View.GONE); }
        });
        aJs("window.Puente.compraConfirmada();");
        aviso("¡Listo! Cavernia queda sin anuncios.");
    }
}
