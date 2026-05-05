package com.gf.erp.tablet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String API = "https://erp-api-7x3d.onrender.com";
    private static final String APP_VERSION = "0.1.3";
    private LinearLayout root;
    private final int NAVY = Color.rgb(0, 31, 42);
    private final int NAVY_2 = Color.rgb(2, 48, 64);
    private final int TEAL = Color.rgb(15, 150, 156);
    private final int GREEN = Color.rgb(22, 163, 74);
    private final int BLUE = Color.rgb(37, 99, 235);
    private final int PURPLE = Color.rgb(124, 58, 237);
    private final int ORANGE = Color.rgb(249, 115, 22);
    private final int TEXT = Color.rgb(2, 8, 23);
    private final int MUTED = Color.rgb(71, 85, 105);
    private final int BG = Color.rgb(246, 248, 251);
    private String sucursal = "computer_army";
    private String usuario = "";
    private String rol = "";
    private JSONArray productos = new JSONArray();
    private long productosCacheMs = 0;
    private final List<JSONObject> carrito = new ArrayList<>();
    private TextView totalView;
    private LinearLayout content;
    private Spinner saleDocType;
    private Spinner saleDocClientType;
    private Spinner salePayState;
    private Spinner salePayMethod;
    private EditText saleClientDoc;
    private EditText saleClientName;
    private EditText saleClientAddress;
    private LinearLayout saleCartList;
    private JSONObject selectedProduct = null;
    private JSONObject selectedInventoryProduct = null;
    private final Map<Integer, Integer> inventoryCounts = new HashMap<>();
    private TextView inventorySummary;
    private ImageView loginAvatar;
    private TextView loginAvatarName;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable avatarTask;
    private final Map<String, Bitmap> imageCache = new HashMap<>();

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        showLogin();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setPadding(dp(8), dp(5), dp(8), dp(5));
        if (bold) v.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return v;
    }

    private Button button(String text, int bg) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackground(rounded(bg, dp(10), 0));
        return b;
    }

    private GradientDrawable rounded(int color, int radius, int strokeColor) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        if (strokeColor != 0) d.setStroke(dp(1), strokeColor);
        return d;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{start, end});
        d.setCornerRadius(radius);
        return d;
    }

    private TextView pill(String text, int bg, int fg) {
        TextView p = label(text, 12, fg, true);
        p.setGravity(Gravity.CENTER);
        p.setBackground(rounded(bg, dp(16), 0));
        return p;
    }

    private EditText input(String hint, boolean pass) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        if (pass) e.setInputType(0x00000081);
        e.setPadding(dp(10), 0, dp(10), 0);
        return e;
    }

    private EditText compactInput(String hint) {
        EditText e = input(hint, false);
        e.setTextSize(12);
        e.setBackground(rounded(Color.WHITE, dp(4), Color.rgb(148, 163, 184)));
        return e;
    }

    private TextView tableHeader(String text) {
        TextView h = label(text, 12, TEXT, true);
        h.setGravity(Gravity.CENTER);
        h.setBackgroundColor(Color.rgb(248, 250, 252));
        return h;
    }

    private LinearLayout fieldRow(View... views) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(3), 0, dp(3));
        for (View v : views) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(42), 1);
            lp.setMargins(dp(4), 0, dp(4), 0);
            row.addView(v, lp);
        }
        return row;
    }

    private TextView cardLabel(String title, String subtitle, int color) {
        TextView row = label(title + "\n" + subtitle, 14, TEXT, true);
        row.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(226, 232, 240)));
        row.setPadding(dp(12), dp(9), dp(12), dp(9));
        return row;
    }

    private void base() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);
    }

    private void showLogin() {
        base();
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setPadding(dp(24), dp(30), dp(24), dp(20));
        scroll.addView(box);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(22), dp(24), dp(22), dp(22));
        card.setBackground(rounded(Color.WHITE, dp(18), Color.rgb(226, 232, 240)));
        box.addView(card, new LinearLayout.LayoutParams(-1, -2));

        loginAvatar = new ImageView(this);
        loginAvatar.setImageResource(getResources().getIdentifier("army", "drawable", getPackageName()));
        card.addView(loginAvatar, new LinearLayout.LayoutParams(dp(132), dp(92)));
        TextView title = label("G&F ERP", 30, TEXT, true);
        title.setGravity(Gravity.CENTER);
        card.addView(title);
        TextView sub = label("Control de ventas, stock y caja", 14, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub);
        loginAvatarName = label("Selecciona tu usuario", 15, TEAL, true);
        loginAvatarName.setGravity(Gravity.CENTER);
        card.addView(loginAvatarName);

        EditText branch = input("Sucursal", false);
        branch.setText("computer_army");
        EditText user = input("Usuario", false);
        EditText pass = input("Contraseña", true);
        card.addView(branch, new LinearLayout.LayoutParams(-1, dp(54)));
        card.addView(user, new LinearLayout.LayoutParams(-1, dp(54)));
        card.addView(pass, new LinearLayout.LayoutParams(-1, dp(54)));
        Button enter = button("Ingresar", TEAL);
        LinearLayout.LayoutParams enterLp = new LinearLayout.LayoutParams(-1, dp(56));
        enterLp.setMargins(0, dp(10), 0, 0);
        card.addView(enter, enterLp);
        Button update = button("Buscar actualizacion", BLUE);
        card.addView(update, new LinearLayout.LayoutParams(-1, dp(48)));
        TextView note = label("Conectado a la API central de Render", 13, MUTED, false);
        note.setGravity(Gravity.CENTER);
        card.addView(note);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1));

        user.addTextChangedListener(new SimpleTextWatcher(() -> scheduleAvatarLoad(user.getText().toString().trim())));
        update.setOnClickListener(v -> checkAndroidUpdate(true));
        handler.postDelayed(() -> checkAndroidUpdate(false), 900);

        enter.setOnClickListener(v -> {
            String u = user.getText().toString().trim();
            String p = pass.getText().toString().trim();
            String s = branch.getText().toString().trim();
            if (u.isEmpty() || p.isEmpty()) {
                msg("Login", "Ingresa usuario y contraseña.");
                return;
            }
            runBusy("Validando usuario...", () -> {
                JSONObject payload = new JSONObject();
                payload.put("usuario", u);
                payload.put("clave", p);
                payload.put("sucursal", s);
                payload.put("empresa", s);
                return request("POST", "/login", null, payload);
            }, r -> {
                if (ok(r) && r.optString("usuario", "").length() > 0) {
                    usuario = r.optString("usuario", u);
                    rol = r.optString("rol", "");
                    sucursal = r.optString("sucursal", s);
                    showShell();
                    showDashboard();
                } else {
                    msg("Login", r.optString("msg", "Credenciales incorrectas o API sin respuesta."));
                }
            });
        });
    }

    private void checkAndroidUpdate(boolean manual) {
        new Thread(() -> {
            try {
                JSONObject r = request("GET", "/app/version", null, null);
                String remote = r.optString("android_version", r.optString("version", ""));
                String url = r.optString("android_url", "");
                String name = r.optString("android_name", "GF_ERP_ANDROID.apk");
                boolean available = compareVersion(remote, APP_VERSION) > 0 && !url.isEmpty();
                runOnUiThread(() -> {
                    if (available) {
                        new AlertDialog.Builder(this)
                                .setTitle("Actualizacion disponible")
                                .setMessage("Nueva version Android: " + remote + "\nTu version: " + APP_VERSION + "\n\n¿Descargar e instalar?")
                                .setPositiveButton("Actualizar", (d, w) -> downloadAndInstallApk(url, name))
                                .setNegativeButton("Luego", null)
                                .show();
                    } else if (manual) {
                        msg("Actualizacion", "Ya tienes la ultima version Android (" + APP_VERSION + ").");
                    }
                });
            } catch (Exception e) {
                if (manual) runOnUiThread(() -> msg("Actualizacion", "No se pudo consultar actualizaciones."));
            }
        }).start();
    }

    private int compareVersion(String a, String b) {
        String[] aa = (a == null ? "" : a).split("\\.");
        String[] bb = (b == null ? "" : b).split("\\.");
        for (int i = 0; i < 3; i++) {
            int av = i < aa.length ? parseInt(aa[i]) : 0;
            int bv = i < bb.length ? parseInt(bb[i]) : 0;
            if (av != bv) return av - bv;
        }
        return 0;
    }

    private void downloadAndInstallApk(String url, String name) {
        runBusy("Descargando actualizacion...", () -> {
            File file = new File(getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir(), name == null || name.isEmpty() ? "GF_ERP_ANDROID.apk" : name);
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(30000);
            c.setReadTimeout(120000);
            try (java.io.InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(file)) {
                byte[] buf = new byte[1024 * 64];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            JSONObject r = new JSONObject();
            r.put("ok", file.length() > 50000);
            r.put("path", file.getAbsolutePath());
            return r;
        }, r -> {
            if (!ok(r)) {
                msg("Actualizacion", "La descarga parece incompleta.");
                return;
            }
            installApkFile(new File(r.optString("path")));
        });
    }

    private void installApkFile(File file) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            msg("Actualizacion", "APK descargada, pero Android bloqueo el instalador. Activa instalar apps desconocidas para G&F ERP.");
        }
    }

    private void scheduleAvatarLoad(String user) {
        if (avatarTask != null) handler.removeCallbacks(avatarTask);
        avatarTask = () -> loadLoginAvatar(user);
        handler.postDelayed(avatarTask, 450);
    }

    private void loadLoginAvatar(String user) {
        if (user == null || user.trim().isEmpty()) {
            loginAvatar.setImageResource(getResources().getIdentifier("army", "drawable", getPackageName()));
            loginAvatarName.setText("Selecciona tu usuario");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject r = request("GET", "/usuarios/perfil", singleParam("usuario", user), null);
                String foundUser = r.optString("usuario", user);
                String foto = r.optString("foto_url", "");
                Bitmap bmp = decodeImageValue(foto);
                runOnUiThread(() -> {
                    loginAvatarName.setText(foundUser);
                    if (bmp != null) loginAvatar.setImageBitmap(bmp);
                    else loginAvatar.setImageResource(getResources().getIdentifier("army", "drawable", getPackageName()));
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> loginAvatarName.setText(user));
            }
        }).start();
    }

    private Bitmap decodeImageValue(String value) {
        try {
            if (value == null || value.trim().isEmpty()) return null;
            value = value.trim();
            byte[] data;
            if (value.startsWith("data:image")) {
                String raw = value.contains(",") ? value.substring(value.indexOf(",") + 1) : "";
                data = Base64.decode(raw, Base64.DEFAULT);
            } else if (value.startsWith("http://") || value.startsWith("https://")) {
                HttpURLConnection c = (HttpURLConnection) new URL(value).openConnection();
                c.setConnectTimeout(9000);
                c.setReadTimeout(9000);
                return BitmapFactory.decodeStream(c.getInputStream());
            } else {
                return null;
            }
            return BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void showShell() {
        base();
        root.setOrientation(LinearLayout.HORIZONTAL);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.VERTICAL);
        nav.setPadding(dp(8), dp(12), dp(8), dp(10));
        nav.setBackground(gradient(NAVY, Color.rgb(0, 62, 72), 0));
        LinearLayout brandBox = new LinearLayout(this);
        brandBox.setOrientation(LinearLayout.HORIZONTAL);
        brandBox.setGravity(Gravity.CENTER_VERTICAL);
        brandBox.addView(pill("G&F", TEAL, Color.WHITE), new LinearLayout.LayoutParams(dp(44), dp(44)));
        brandBox.addView(label("  G&F ERP", 18, Color.WHITE, true), new LinearLayout.LayoutParams(0, dp(52), 1));
        nav.addView(brandBox, new LinearLayout.LayoutParams(-1, dp(62)));
        addNavButton(nav, "Panel", v -> showDashboard(), true);
        addNavButton(nav, "Ventas", v -> showSales(), true);
        addNavButton(nav, "Inventario", v -> showInventoryPc(), true);
        addNavButton(nav, "Compras", v -> msg("Compras", "Modulo disponible en PC; se completara en Android."), true);
        addNavButton(nav, "Clientes", v -> showClients(), true);
        addNavButton(nav, "Productos", v -> showProducts(), true);
        addNavButton(nav, "Documentos", v -> showDocuments(), true);
        addNavButton(nav, "Caja", v -> showCash(), true);
        addNavButton(nav, "Usuarios", v -> showUsers(), true);
        addNavButton(nav, "Garantias", v -> showGarantias(), true);
        addNavButton(nav, "Registro", v -> showAudit(), true);
        addNavButton(nav, "Pagina Web", v -> showWeb(), true);
        addNavButton(nav, "Configuracion", v -> showSettings(), true);
        addNavButton(nav, "Ayuda", v -> msg("Ayuda", "G&F ERP Android conectado a Render."), false);
        addNavButton(nav, "Cerrar sesion", v -> showLogin(), false);
        ScrollView navScroll = new ScrollView(this);
        navScroll.setFillViewport(true);
        navScroll.setBackground(gradient(NAVY, Color.rgb(0, 62, 72), 0));
        navScroll.addView(nav, new ScrollView.LayoutParams(-1, -2));
        root.addView(navScroll, new LinearLayout.LayoutParams(dp(isWideLayout() ? 230 : 176), -1));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setBackgroundColor(BG);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(16), dp(8), dp(16), dp(8));
        top.setBackgroundColor(Color.WHITE);
        EditText globalSearch = compactInput("Buscar");
        top.addView(globalSearch, new LinearLayout.LayoutParams(0, dp(46), 1));
        top.addView(label("  Computer Army  ", 13, TEXT, true), new LinearLayout.LayoutParams(-2, dp(46)));
        ImageView avatar = new ImageView(this);
        avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatar.setBackground(rounded(Color.rgb(226, 232, 240), dp(8), Color.rgb(203, 213, 225)));
        top.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
        top.addView(label("  " + usuario + "  -  " + rol, 13, TEXT, true), new LinearLayout.LayoutParams(-2, dp(46)));
        main.addView(top, new LinearLayout.LayoutParams(-1, dp(64)));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        main.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(main, new LinearLayout.LayoutParams(0, -1, 1));
        loadUserAvatarInto(avatar, usuario);
    }

    private boolean isWideLayout() {
        return getResources().getDisplayMetrics().widthPixels >= dp(900);
    }

    private void addNavButton(LinearLayout nav, String text, View.OnClickListener action, boolean mainButton) {
        Button b = button(text, mainButton ? NAVY_2 : NAVY);
        b.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        b.setPadding(dp(16), 0, dp(8), 0);
        b.setTextSize(13);
        b.setOnClickListener(action);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.setMargins(0, dp(3), 0, dp(3));
        nav.addView(b, lp);
    }
    private void clearContent() {
        content.removeAllViews();
    }

    private void loadUserAvatarInto(ImageView view, String userName) {
        if (view == null || userName == null || userName.trim().isEmpty()) return;
        new Thread(() -> {
            try {
                JSONObject r = request("GET", "/usuarios/perfil", singleParam("usuario", userName), null);
                Bitmap b = decodeImageValue(r.optString("foto_url", ""));
                if (b != null) runOnUiThread(() -> view.setImageBitmap(b));
            } catch (Exception ignored) {}
        }).start();
    }

    private String productImageValue(JSONObject p) {
        String v = p.optString("imagen_url", "");
        if (v == null || v.trim().isEmpty()) v = p.optString("image_url", "");
        if (v == null || v.trim().isEmpty()) v = p.optString("imagen", "");
        return v == null ? "" : v.trim();
    }

    private void loadProductImageInto(ImageView view, JSONObject p) {
        String key = productImageValue(p);
        if (key.isEmpty()) {
            view.setImageBitmap(productPlaceholderBitmap(productInitial(p), dp(86), dp(76), colorForProduct(p)));
            return;
        }
        Bitmap cached = imageCache.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        view.setImageBitmap(productPlaceholderBitmap(productInitial(p), dp(86), dp(76), colorForProduct(p)));
        new Thread(() -> {
            Bitmap b = decodeImageValue(key);
            if (b != null) {
                imageCache.put(key, b);
                runOnUiThread(() -> view.setImageBitmap(b));
            }
        }).start();
    }

    private Bitmap productPlaceholderBitmap(String text, int w, int h, int color) {
        Bitmap bm = Bitmap.createBitmap(Math.max(1, w), Math.max(1, h), Bitmap.Config.ARGB_8888);
        android.graphics.Canvas c = new android.graphics.Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        c.drawRoundRect(0, 0, w, h, dp(9), dp(9), p);
        p.setColor(Color.WHITE);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(dp(13));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(text == null ? "PC" : text, w / 2f, h / 2f + dp(5), p);
        return bm;
    }

    private void showDashboard() {
        runBusy("Cargando panel...", () -> request("GET", "/dashboard", params(), null), r -> {
            clearContent();
            content.addView(sectionTitle("Panel", "Resumen general de tu negocio"));
            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(2);
            grid.setPadding(dp(8), dp(8), dp(8), dp(8));
            addMetric(grid, "Ventas", money(r.optDouble("total_ventas", 0)), "$", TEAL);
            addMetric(grid, "Productos", String.valueOf(r.optInt("productos", 0)), "P", GREEN);
            addMetric(grid, "Clientes", String.valueOf(r.optInt("clientes", 0)), "C", ORANGE);
            addMetric(grid, "Caja", money(r.optDouble("saldo_caja", r.optDouble("total_ventas", 0))), "S/", PURPLE);
            addMetric(grid, "Stock bajo", String.valueOf(r.optInt("stock_bajo", 0)), "!", Color.rgb(220, 38, 38));
            addMetric(grid, "Por cobrar", String.valueOf(r.optInt("facturas_por_cobrar", 0)), "D", BLUE);
            ScrollView s = new ScrollView(this);
            s.addView(grid);
            content.addView(s, new LinearLayout.LayoutParams(-1, 0, 1));
        });
    }

    private LinearLayout sectionTitle(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(12), dp(12), dp(6));
        box.addView(label(title, 25, TEXT, true));
        box.addView(label(subtitle, 13, MUTED, false));
        return box;
    }

    private void addMetric(GridLayout grid, String title, String value, String icon, int iconColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(10), dp(10), dp(10), dp(10));
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(rounded(Color.WHITE, dp(14), Color.rgb(226, 232, 240)));
        TextView badge = pill(icon, iconColor, Color.WHITE);
        card.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label(title, 13, MUTED, false));
        texts.addView(label(value, 18, TEXT, true));
        card.addView(texts, new LinearLayout.LayoutParams(0, -1, 1));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = getResources().getDisplayMetrics().widthPixels / 2 - dp(14);
        lp.height = dp(104);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        grid.addView(card, lp);
    }

    private void loadProducts(ProductDone done) {
        if (productos.length() > 0 && System.currentTimeMillis() - productosCacheMs < 180000) {
            done.done(productos);
            return;
        }
        runBusy("Cargando productos...", () -> requestArray("/productos", params()), r -> {
            productos = r;
            productosCacheMs = System.currentTimeMillis();
            done.done(productos);
        });
    }

    private void showProducts() {
        loadProducts(arr -> {
            clearContent();
            selectedProduct = null;
            EditText search = input("Buscar producto", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(sectionTitle("Productos", "Catalogo sincronizado con la API"));

            LinearLayout form = new LinearLayout(this);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(dp(10), dp(8), dp(10), dp(8));
            form.setBackground(rounded(Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
            EditText fNombre = compactInput("Nombre");
            EditText fCategoria = compactInput("Categoria");
            EditText fMarca = compactInput("Marca");
            EditText fModelo = compactInput("Modelo");
            EditText fCompra = compactInput("P.Compra");
            EditText fVenta = compactInput("P.Venta");
            EditText fStock = compactInput("Stock");
            EditText fImagen = compactInput("Imagen URL");
            form.addView(fieldRow(fNombre, fCategoria, fMarca, fModelo));
            form.addView(fieldRow(fCompra, fVenta, fStock, fImagen));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button saveNew = button("Guardar Producto Nuevo", TEAL);
            Button editSel = button("Editar seleccionado", GREEN);
            Button delSel = button("Eliminar seleccionado", Color.rgb(220, 38, 38));
            Button syncSel = button("Sincronizar Woo", PURPLE);
            actions.addView(saveNew, new LinearLayout.LayoutParams(0, dp(44), 1));
            actions.addView(editSel, new LinearLayout.LayoutParams(0, dp(44), 1));
            actions.addView(delSel, new LinearLayout.LayoutParams(0, dp(44), 1));
            actions.addView(syncSel, new LinearLayout.LayoutParams(0, dp(44), 1));
            form.addView(actions);
            content.addView(form, new LinearLayout.LayoutParams(-1, -2));

            TextView tip = label("Tip: toca un producto para seleccionarlo y llenar el formulario.", 12, MUTED, false);
            content.addView(tip);
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(52)));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            Runnable paint = () -> paintProductsTable(list, search.getText().toString(), fNombre, fCategoria, fMarca, fModelo, fCompra, fVenta, fStock, fImagen);
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            paint.run();
            saveNew.setOnClickListener(v -> saveProductFromForm(null, fNombre, fCategoria, fMarca, fModelo, fCompra, fVenta, fStock, fImagen));
            editSel.setOnClickListener(v -> {
                if (selectedProduct == null) msg("Productos", "Selecciona un producto.");
                else saveProductFromForm(selectedProduct, fNombre, fCategoria, fMarca, fModelo, fCompra, fVenta, fStock, fImagen);
            });
            delSel.setOnClickListener(v -> {
                if (selectedProduct == null) msg("Productos", "Selecciona un producto.");
                else deleteProduct(selectedProduct);
            });
            syncSel.setOnClickListener(v -> {
                if (selectedProduct == null) msg("Productos", "Selecciona un producto.");
                else syncProductWeb(selectedProduct);
            });
        });
    }

    private void paintProductsTable(LinearLayout list, String q, EditText nombre, EditText categoria, EditText marca, EditText modelo, EditText compra, EditText venta, EditText stockField, EditText imagen) {
        list.removeAllViews();
        GridLayout header = new GridLayout(this);
        header.setColumnCount(7);
        for (String h : new String[]{"ID", "Nombre", "Categoria", "Marca", "Modelo", "P.Venta", "Stock"}) {
            header.addView(tableHeader(h), new ViewGroup.LayoutParams(getResources().getDisplayMetrics().widthPixels / 7, dp(34)));
        }
        list.addView(header);
        q = q.toLowerCase();
        int shown = 0;
        for (int i = 0; i < productos.length() && shown < 180; i++) {
            JSONObject p = productos.optJSONObject(i);
            String text = (p.optString("nombre") + " " + p.optString("categoria") + " " + p.optString("marca") + " " + p.optString("modelo")).toLowerCase();
            if (!q.isEmpty() && !text.contains(q)) continue;
            TextView row = cardLabel(p.optString("id") + "  |  " + p.optString("nombre"), p.optString("categoria") + " | " + p.optString("marca") + " | " + p.optString("modelo") + " | " + money(p.optDouble("precio_venta")) + " | Stock: " + p.optInt("stock"), TEAL);
            int stock = p.optInt("stock");
            row.setBackground(rounded(stock <= 0 ? Color.rgb(254, 226, 226) : stock <= 5 ? Color.rgb(254, 249, 195) : Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
            row.setOnClickListener(v -> {
                selectedProduct = p;
                nombre.setText(p.optString("nombre"));
                categoria.setText(p.optString("categoria"));
                marca.setText(p.optString("marca"));
                modelo.setText(p.optString("modelo"));
                compra.setText(String.valueOf(p.optDouble("precio_compra", 0)));
                venta.setText(String.valueOf(p.optDouble("precio_venta", 0)));
                stockField.setText(String.valueOf(p.optInt("stock", 0)));
                imagen.setText(p.optString("imagen_url"));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(64));
            lp.setMargins(0, 0, 0, dp(4));
            list.addView(row, lp);
            shown++;
        }
    }

    private void saveProductFromForm(JSONObject product, EditText nombre, EditText categoria, EditText marca, EditText modelo, EditText compra, EditText venta, EditText stock, EditText imagen) {
        boolean edit = product != null;
        runBusy("Guardando producto...", () -> {
            JSONObject payload = new JSONObject();
            payload.put("nombre", nombre.getText().toString().trim());
            payload.put("categoria", categoria.getText().toString().trim());
            payload.put("marca", marca.getText().toString().trim());
            payload.put("modelo", modelo.getText().toString().trim());
            payload.put("precio_compra", parseDouble(compra.getText().toString()));
            payload.put("precio_venta", parseDouble(venta.getText().toString()));
            payload.put("stock", parseInt(stock.getText().toString()));
            payload.put("imagen_url", imagen.getText().toString().trim());
            payload.put("sucursal", sucursal);
            return request(edit ? "PUT" : "POST", edit ? "/productos/" + product.optInt("id") : "/productos", edit ? params() : null, payload);
        }, r -> {
            if (ok(r)) {
                productos = new JSONArray();
                productosCacheMs = 0;
                msg("Productos", "Producto guardado.");
                showProducts();
            } else msg("Productos", r.optString("msg", "No se pudo guardar."));
        });
    }

    private void paintProducts(LinearLayout list, String q) {
        list.removeAllViews();
        q = q.toLowerCase();
        int shown = 0;
        for (int i = 0; i < productos.length() && shown < 100; i++) {
            JSONObject p = productos.optJSONObject(i);
            String text = (p.optString("nombre") + " " + p.optString("categoria") + " " + p.optString("marca") + " " + p.optString("modelo")).toLowerCase();
            if (!q.isEmpty() && !text.contains(q)) continue;
            LinearLayout row = productCard(p, true, v -> showProductOptions(p));
            list.addView(row, new LinearLayout.LayoutParams(-1, dp(94)));
            shown++;
        }
    }

    private void showProductOptions(JSONObject p) {
        String[] opts = {"Editar producto", "Eliminar producto", "Sincronizar con web", "Ajustar stock", "Ver series"};
        new AlertDialog.Builder(this)
                .setTitle(p.optString("nombre"))
                .setItems(opts, (d, which) -> {
                    if (which == 0) showProductEditor(p);
                    if (which == 1) deleteProduct(p);
                    if (which == 2) syncProductWeb(p);
                    if (which == 3) showAdjustStockDialog(p);
                    if (which == 4) showProductSeries(p);
                })
                .show();
    }

    private void showProductEditor(JSONObject p) {
        boolean edit = p != null;
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        EditText nombre = input("Nombre", false);
        EditText categoria = input("Categoria", false);
        EditText marca = input("Marca", false);
        EditText modelo = input("Modelo", false);
        EditText pc = input("Precio compra", false);
        EditText pv = input("Precio venta", false);
        EditText stock = input("Stock", false);
        EditText img = input("Imagen URL", false);
        if (edit) {
            nombre.setText(p.optString("nombre"));
            categoria.setText(p.optString("categoria"));
            marca.setText(p.optString("marca"));
            modelo.setText(p.optString("modelo"));
            pc.setText(String.valueOf(p.optDouble("precio_compra", 0)));
            pv.setText(String.valueOf(p.optDouble("precio_venta", 0)));
            stock.setText(String.valueOf(p.optInt("stock", 0)));
            img.setText(p.optString("imagen_url"));
        }
        box.addView(nombre); box.addView(categoria); box.addView(marca); box.addView(modelo);
        box.addView(pc); box.addView(pv); box.addView(stock); box.addView(img);
        new AlertDialog.Builder(this)
                .setTitle(edit ? "Editar producto" : "Nuevo producto")
                .setView(box)
                .setPositiveButton("Guardar", (d, w) -> {
                    runBusy("Guardando producto...", () -> {
                        JSONObject payload = new JSONObject();
                        payload.put("nombre", nombre.getText().toString().trim());
                        payload.put("categoria", categoria.getText().toString().trim());
                        payload.put("marca", marca.getText().toString().trim());
                        payload.put("modelo", modelo.getText().toString().trim());
                        payload.put("precio_compra", parseDouble(pc.getText().toString()));
                        payload.put("precio_venta", parseDouble(pv.getText().toString()));
                        payload.put("stock", parseInt(stock.getText().toString()));
                        payload.put("imagen_url", img.getText().toString().trim());
                        payload.put("sucursal", sucursal);
                        return request(edit ? "PUT" : "POST", edit ? "/productos/" + p.optInt("id") : "/productos", edit ? params() : null, payload);
                    }, r -> {
                        if (ok(r)) {
                            productos = new JSONArray();
                            productosCacheMs = 0;
                            msg("Productos", "Producto guardado.");
                            showProducts();
                        } else msg("Productos", r.optString("msg", "No se pudo guardar."));
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteProduct(JSONObject p) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar producto")
                .setMessage("¿Eliminar " + p.optString("nombre") + "?")
                .setPositiveButton("Eliminar", (d, w) -> runBusy("Eliminando producto...", () -> request("DELETE", "/productos/" + p.optInt("id"), params(), null), r -> {
                    if (ok(r)) {
                        productos = new JSONArray();
                        productosCacheMs = 0;
                        msg("Productos", "Producto eliminado.");
                        showProducts();
                    } else msg("Productos", r.optString("msg", "No se pudo eliminar."));
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void syncProductWeb(JSONObject p) {
        runBusy("Sincronizando con web...", () -> request("POST", "/web/woocommerce/sync-product/" + p.optInt("id"), params(), null), r -> {
            if (ok(r)) msg("Web", r.optString("msg", "Producto sincronizado."));
            else msg("Web", r.optString("msg", "No se pudo sincronizar."));
        });
    }

    private LinearLayout productCard(JSONObject p, boolean clickable, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(rounded(Color.WHITE, dp(10), Color.rgb(226, 232, 240)));

        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackground(rounded(Color.rgb(248, 250, 252), dp(8), Color.rgb(226, 232, 240)));
        row.addView(img, new LinearLayout.LayoutParams(dp(74), dp(68)));
        loadProductImageInto(img, p);

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(10), 0, dp(8), 0);
        info.addView(label(p.optString("nombre"), 14, TEXT, true));
        info.addView(label(p.optString("categoria") + " - " + p.optString("marca") + " " + p.optString("modelo"), 12, MUTED, false));
        int stock = p.optInt("stock");
        int stockColor = stock <= 0 ? Color.rgb(220, 38, 38) : stock <= 3 ? ORANGE : GREEN;
        info.addView(label("Stock: " + stock, 12, stockColor, true));
        row.addView(info, new LinearLayout.LayoutParams(0, -1, 1));

        TextView price = label(money(p.optDouble("precio_venta")), 15, TEAL, true);
        price.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        row.addView(price, new LinearLayout.LayoutParams(dp(105), -1));
        LinearLayout.LayoutParams margins = new LinearLayout.LayoutParams(-1, dp(94));
        margins.setMargins(0, 0, 0, dp(8));
        row.setLayoutParams(margins);
        if (clickable && listener != null) row.setOnClickListener(listener);
        return row;
    }

    private LinearLayout productTile(JSONObject p, View.OnClickListener listener) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(8), dp(8), dp(8), dp(8));
        card.setBackground(rounded(Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        img.setBackground(rounded(Color.rgb(248, 250, 252), dp(8), Color.rgb(226, 232, 240)));
        card.addView(img, new LinearLayout.LayoutParams(-1, dp(94)));
        loadProductImageInto(img, p);
        TextView name = label(shortText(p.optString("nombre"), 42), 12, TEXT, true);
        name.setGravity(Gravity.CENTER);
        card.addView(name, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView price = label(money(p.optDouble("precio_venta")), 15, TEXT, true);
        price.setGravity(Gravity.CENTER);
        card.addView(price, new LinearLayout.LayoutParams(-1, dp(30)));
        int stock = p.optInt("stock");
        int stockColor = stock <= 0 ? Color.rgb(220, 38, 38) : stock <= 3 ? Color.RED : GREEN;
        TextView stockView = label("Stock: " + stock, 12, stockColor, true);
        stockView.setGravity(Gravity.CENTER);
        card.addView(stockView, new LinearLayout.LayoutParams(-1, dp(26)));
        card.setOnClickListener(listener);
        return card;
    }
    private String productInitial(JSONObject p) {
        String t = (p.optString("categoria") + " " + p.optString("nombre")).toUpperCase();
        if (t.contains("RAM") || t.contains("MEMORIA")) return "RAM";
        if (t.contains("SSD") || t.contains("NVME") || t.contains("DISCO")) return "SSD";
        if (t.contains("GRAFICA") || t.contains("RTX") || t.contains("GTX")) return "GPU";
        if (t.contains("MONITOR")) return "MON";
        return "PC";
    }

    private int colorForProduct(JSONObject p) {
        String t = (p.optString("categoria") + " " + p.optString("nombre")).toUpperCase();
        if (t.contains("RAM")) return Color.rgb(17, 24, 39);
        if (t.contains("SSD") || t.contains("NVME")) return Color.rgb(51, 65, 85);
        if (t.contains("GRAFICA") || t.contains("RTX")) return GREEN;
        if (t.contains("MONITOR")) return BLUE;
        return TEAL;
    }

    private void showSales() {
        loadProducts(arr -> {
            carrito.clear();
            clearContent();
            EditText search = input("Buscar producto", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(sectionTitle("Nueva venta", "Busca, toca y cobra desde el celular"));

            LinearLayout saleOpts = new LinearLayout(this);
            saleOpts.setOrientation(LinearLayout.HORIZONTAL);
            saleDocType = new Spinner(this);
            saleDocType.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"PROFORMA", "NOTA DE VENTA", "BOLETA", "FACTURA"}));
            saleDocClientType = new Spinner(this);
            saleDocClientType.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"DNI", "RUC"}));
            saleOpts.addView(saleDocType, new LinearLayout.LayoutParams(0, dp(48), 1.3f));
            saleOpts.addView(saleDocClientType, new LinearLayout.LayoutParams(0, dp(48), 0.8f));
            content.addView(saleOpts);

            LinearLayout clientLine = new LinearLayout(this);
            clientLine.setOrientation(LinearLayout.HORIZONTAL);
            saleClientDoc = input("DNI/RUC", false);
            saleClientName = input("Cliente o USUARIO X", false);
            saleClientDoc.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            saleClientName.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            clientLine.addView(saleClientDoc, new LinearLayout.LayoutParams(0, dp(48), 0.9f));
            clientLine.addView(saleClientName, new LinearLayout.LayoutParams(0, dp(48), 1.4f));
            content.addView(clientLine);

            saleClientAddress = input("Direccion", false);
            saleClientAddress.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(saleClientAddress, new LinearLayout.LayoutParams(-1, dp(48)));
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(48)));

            LinearLayout body = new LinearLayout(this);
            body.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout left = new LinearLayout(this);
            left.setOrientation(LinearLayout.VERTICAL);
            ScrollView leftScroll = new ScrollView(this);
            leftScroll.addView(left);
            LinearLayout right = new LinearLayout(this);
            right.setOrientation(LinearLayout.VERTICAL);
            totalView = label("Total: S/ 0.00", 18, Color.rgb(15, 118, 110), true);
            totalView.setGravity(Gravity.RIGHT);
            Button cobrar = button("Cobrar / Emitir", GREEN);
            right.addView(label("Carrito", 18, TEXT, true));
            saleCartList = new LinearLayout(this);
            saleCartList.setOrientation(LinearLayout.VERTICAL);
            ScrollView cartScroll = new ScrollView(this);
            cartScroll.addView(saleCartList);
            right.addView(cartScroll, new LinearLayout.LayoutParams(-1, 0, 1));
            right.addView(totalView, new LinearLayout.LayoutParams(-1, dp(48)));
            salePayState = new Spinner(this);
            salePayState.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"PAGADO", "CREDITO", "DEUDA"}));
            salePayMethod = new Spinner(this);
            salePayMethod.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"EFECTIVO", "TRANSFERENCIA", "YAPE", "PLIN", "TARJETA"}));
            right.addView(salePayState, new LinearLayout.LayoutParams(-1, dp(44)));
            right.addView(salePayMethod, new LinearLayout.LayoutParams(-1, dp(44)));
            right.addView(cobrar, new LinearLayout.LayoutParams(-1, dp(58)));
            body.addView(leftScroll, new LinearLayout.LayoutParams(0, -1, 1.4f));
            body.addView(right, new LinearLayout.LayoutParams(0, -1, 1));
            content.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));

            Runnable paint = () -> paintSaleProducts(left, saleCartList, search.getText().toString());
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            paint.run();
            cobrar.setOnClickListener(v -> emitSale());
        });
    }

    private void paintSaleProducts(LinearLayout list, LinearLayout cartList, String q) {
        list.removeAllViews();
        q = q.toLowerCase();
        GridLayout grid = new GridLayout(this);
        int columns = isWideLayout() ? 4 : 2;
        grid.setColumnCount(columns);
        grid.setPadding(dp(4), dp(4), dp(4), dp(4));
        int tileW = Math.max(dp(145), (getResources().getDisplayMetrics().widthPixels - dp(isWideLayout() ? 650 : 360)) / columns);
        int shown = 0;
        for (int i = 0; i < productos.length() && shown < 120; i++) {
            JSONObject p = productos.optJSONObject(i);
            String text = (p.optString("nombre") + " " + p.optString("categoria") + " " + p.optString("marca") + " " + p.optString("modelo") + " " + p.optString("sku")).toLowerCase();
            if (!q.isEmpty() && !text.contains(q)) continue;
            LinearLayout tile = productTile(p, v -> {
                addCart(p);
                paintCart(cartList);
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = tileW;
            lp.height = dp(210);
            lp.setMargins(dp(5), dp(5), dp(5), dp(5));
            grid.addView(tile, lp);
            shown++;
        }
        if (shown == 0) {
            list.addView(cardLabel("Sin productos", "No hay coincidencias para la busqueda.", ORANGE), new LinearLayout.LayoutParams(-1, dp(80)));
        } else {
            list.addView(grid);
        }
    }
    private void addCart(JSONObject p) {
        try {
            for (JSONObject it : carrito) {
                if (it.optInt("id") == p.optInt("id")) {
                    int qty = it.optInt("cantidad") + 1;
                    double price = it.optDouble("precio");
                    it.put("cantidad", qty);
                    it.put("total", qty * price);
                    return;
                }
            }
            JSONObject it = new JSONObject();
            it.put("id", p.optInt("id"));
            it.put("producto_id", p.optInt("id"));
            it.put("nombre", p.optString("nombre"));
            it.put("marca", p.optString("marca"));
            it.put("modelo", p.optString("modelo"));
            it.put("cantidad", 1);
            it.put("precio", p.optDouble("precio_venta"));
            it.put("total", p.optDouble("precio_venta"));
            it.put("series_texto", "");
            carrito.add(it);
        } catch (Exception ignored) {}
    }

    private void paintCart(LinearLayout list) {
        list.removeAllViews();
        double total = 0;
        for (int i = 0; i < carrito.size(); i++) {
            JSONObject it = carrito.get(i);
            total += it.optDouble("total");
            TextView row = label(it.optInt("cantidad") + " x " + it.optString("nombre") + "\n" + money(it.optDouble("total")) + (it.optString("series_texto").isEmpty() ? "" : "\nSN: " + it.optString("series_texto")), 13, TEXT, true);
            row.setBackground(rounded(Color.rgb(248, 250, 252), dp(10), Color.rgb(226, 232, 240)));
            int idx = i;
            row.setOnClickListener(v -> editCartItem(idx));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(66));
            lp.setMargins(0, 0, 0, dp(6));
            list.addView(row, lp);
        }
        totalView.setText("Total: " + money(total));
    }

    private void editCartItem(int idx) {
        if (idx < 0 || idx >= carrito.size()) return;
        JSONObject it = carrito.get(idx);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        EditText name = input("Nombre", false);
        EditText qty = input("Cantidad", false);
        EditText price = input("Precio", false);
        EditText serie = input("Serie / series", false);
        name.setText(it.optString("nombre"));
        qty.setText(String.valueOf(it.optInt("cantidad", 1)));
        price.setText(String.valueOf(it.optDouble("precio", 0)));
        serie.setText(it.optString("series_texto"));
        box.addView(name); box.addView(qty); box.addView(price); box.addView(serie);
        new AlertDialog.Builder(this)
                .setTitle("Editar item")
                .setView(box)
                .setPositiveButton("Guardar", (d, w) -> {
                    try {
                        int q = Math.max(1, Integer.parseInt(qty.getText().toString().trim()));
                        double pr = Double.parseDouble(price.getText().toString().trim());
                        it.put("nombre", name.getText().toString().trim());
                        it.put("cantidad", q);
                        it.put("precio", pr);
                        it.put("total", q * pr);
                        it.put("series_texto", serie.getText().toString().trim());
                        paintCart(saleCartList);
                    } catch (Exception e) {
                        msg("Item", "Datos invalidos.");
                    }
                })
                .setNegativeButton("Eliminar", (d, w) -> {
                    carrito.remove(idx);
                    paintCart(saleCartList);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void emitSale() {
        if (carrito.isEmpty()) {
            msg("Venta", "Agrega productos al carrito.");
            return;
        }
        String tipo = String.valueOf(saleDocType.getSelectedItem());
        String cliente = saleClientName.getText().toString().trim();
        if (!tipo.equals("PROFORMA") && (cliente.isEmpty() || saleClientDoc.getText().toString().trim().isEmpty())) {
            msg("Venta", "Para boleta/factura/nota ingresa documento y cliente.");
            return;
        }
        runBusy("Emitiendo documento...", () -> {
            JSONObject payload = new JSONObject();
            payload.put("tipo", tipo);
            payload.put("cliente_nombre", cliente.isEmpty() ? "USUARIO X" : cliente);
            payload.put("tipo_documento_cliente", tipo.equals("PROFORMA") ? "" : String.valueOf(saleDocClientType.getSelectedItem()));
            payload.put("numero_documento_cliente", tipo.equals("PROFORMA") ? "" : saleClientDoc.getText().toString().trim());
            payload.put("direccion_cliente", saleClientAddress.getText().toString().trim());
            payload.put("usuario_emisor", usuario);
            payload.put("estado_pago", String.valueOf(salePayState.getSelectedItem()));
            payload.put("metodo_pago", String.valueOf(salePayMethod.getSelectedItem()));
            payload.put("sucursal", sucursal);
            JSONArray items = new JSONArray();
            for (JSONObject it : carrito) items.put(it);
            payload.put("items", items);
            return request("POST", "/ventas", null, payload);
        }, r -> {
            if (ok(r)) {
                msg("Venta", "Documento emitido\n" + r.optString("numero") + "\n" + money(r.optDouble("total")));
                productos = new JSONArray();
                productosCacheMs = 0;
                showSales();
            } else {
                msg("Venta", r.optString("msg", "No se pudo emitir."));
            }
        });
    }

    private void showCash() {
        runBusy("Cargando caja...", () -> requestArray("/documentos", params()), docs -> {
            clearContent();
            content.addView(sectionTitle("Caja", "Documentos emitidos y estado de pago"));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            for (int i = 0; i < docs.length() && i < 150; i++) {
                JSONObject d = docs.optJSONObject(i);
                String estado = d.optString("estado_pago", "PAGADO").toUpperCase();
                int bg = estado.equals("PAGADO") ? Color.rgb(220, 252, 231) : estado.equals("CREDITO") ? Color.rgb(254, 249, 195) : Color.rgb(254, 226, 226);
                TextView row = label(d.optString("tipo") + " " + d.optString("numero") + "\n" + d.optString("cliente_nombre") + " · " + estado + " · " + money(d.optDouble("total")), 14, TEXT, true);
                row.setBackground(rounded(bg, dp(12), Color.rgb(226, 232, 240)));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(78));
                lp.setMargins(0, 0, 0, dp(7));
                list.addView(row, lp);
            }
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        });
    }

    private void showDocuments() {
        runBusy("Cargando documentos...", () -> requestArray("/documentos", params()), docs -> {
            clearContent();
            content.addView(sectionTitle("Documentos", "Boletas, facturas, notas de venta y proformas"));
            EditText search = input("Buscar numero, cliente o usuario", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));

            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

            Runnable paint = () -> {
                list.removeAllViews();
                String q = search.getText().toString().toLowerCase();
                for (int i = 0; i < docs.length() && i < 200; i++) {
                    JSONObject d = docs.optJSONObject(i);
                    String text = (d.optString("tipo") + " " + d.optString("numero") + " " + d.optString("cliente_nombre") + " " + d.optString("usuario_emisor")).toLowerCase();
                    if (!q.isEmpty() && !text.contains(q)) continue;
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(10), dp(8), dp(10), dp(8));
                    row.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(226, 232, 240)));
                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.addView(label(d.optString("tipo") + " " + d.optString("numero"), 15, TEXT, true));
                    info.addView(label(d.optString("cliente_nombre") + " · " + money(d.optDouble("total")), 13, MUTED, false));
                    info.addView(label(d.optString("estado_pago", "PAGADO") + " · " + d.optString("fecha_emision"), 12, TEAL, true));
                    row.addView(info, new LinearLayout.LayoutParams(0, -1, 1));
                    Button pdf = button("PDF", Color.rgb(220, 38, 38));
                    pdf.setOnClickListener(v -> openDocumentPdf(d));
                    row.addView(pdf, new LinearLayout.LayoutParams(dp(74), dp(54)));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(96));
                    lp.setMargins(0, 0, 0, dp(8));
                    list.addView(row, lp);
                }
            };
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            paint.run();
        });
    }

    private void openDocumentPdf(JSONObject doc) {
        int id = doc.optInt("id");
        runBusy("Generando PDF...", () -> {
            JSONArray detail = requestArray("/documentos/" + id, new JSONObject());
            File pdf = createDocumentPdf(doc, detail);
            JSONObject out = new JSONObject();
            out.put("ok", true);
            out.put("path", pdf.getAbsolutePath());
            return out;
        }, r -> {
            if (!ok(r)) {
                msg("PDF", r.optString("msg", "No se pudo generar el PDF."));
                return;
            }
            openPdfFile(new File(r.optString("path")));
        });
    }

    private File createDocumentPdf(JSONObject doc, JSONArray detail) throws Exception {
        int pageW = 595;
        int pageH = 842;
        PdfDocument pdf = new PdfDocument();
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint bold = new Paint(Paint.ANTI_ALIAS_FLAG);
        bold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        File out = new File(getCacheDir(), safeFile(doc.optString("numero", "documento")) + ".pdf");

        int itemIndex = 0;
        int rowsPerPage = 18;
        int pageNo = 1;
        do {
            PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(pageW, pageH, pageNo).create());
            android.graphics.Canvas c = page.getCanvas();
            c.drawColor(Color.WHITE);

            bold.setTextSize(16);
            p.setTextSize(9);
            bold.setColor(Color.BLACK);
            p.setColor(Color.BLACK);
            c.drawText("COMPUTER ARMY / G&F ERP", 34, 38, bold);
            c.drawText("Documento emitido desde ERP central", 34, 55, p);
            c.drawText("RUC 20611068701", 405, 38, p);
            bold.setTextSize(18);
            c.drawText(doc.optString("tipo", "DOCUMENTO"), 405, 70, bold);
            bold.setTextSize(13);
            c.drawText(doc.optString("numero", ""), 405, 94, bold);

            p.setTextSize(9);
            bold.setTextSize(9);
            c.drawText("CLIENTE", 34, 125, bold);
            c.drawText(doc.optString("cliente_nombre", "USUARIO X"), 105, 125, p);
            c.drawText("DOCUMENTO", 34, 143, bold);
            c.drawText(doc.optString("documento_cliente", ""), 105, 143, p);
            c.drawText("FECHA", 405, 125, bold);
            c.drawText(doc.optString("fecha_emision", ""), 455, 125, p);
            c.drawText("VENDEDOR", 405, 143, bold);
            c.drawText(doc.optString("usuario_emisor", ""), 455, 143, p);

            int tableY = 176;
            p.setStyle(Paint.Style.STROKE);
            c.drawRect(28, tableY, 567, 660, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.BLACK);
            c.drawRect(28, tableY, 567, tableY + 24, p);
            p.setColor(Color.WHITE);
            bold.setTextSize(8);
            c.drawText("N°", 36, tableY + 16, bold);
            c.drawText("CODIGO", 70, tableY + 16, bold);
            c.drawText("DESCRIPCION", 135, tableY + 16, bold);
            c.drawText("CANT.", 405, tableY + 16, bold);
            c.drawText("TOTAL", 463, tableY + 16, bold);
            c.drawText("P.UNIT.", 515, tableY + 16, bold);
            p.setColor(Color.BLACK);
            bold.setColor(Color.BLACK);

            int y = tableY + 46;
            int printed = 0;
            while (itemIndex < detail.length() && printed < rowsPerPage) {
                JSONObject it = detail.optJSONObject(itemIndex);
                int global = itemIndex + 1;
                p.setTextSize(8);
                bold.setTextSize(8);
                c.drawText(String.valueOf(global), 38, y, p);
                c.drawText(String.valueOf(it.optInt("producto_id", 0)), 70, y, p);
                c.drawText(shortText(it.optString("descripcion", "") + " " + it.optString("marca", "") + " " + it.optString("modelo", ""), 58), 135, y, bold);
                String serie = it.optString("series_texto", "");
                if (!serie.isEmpty()) {
                    p.setTextSize(7);
                    c.drawText("SN: " + shortText(serie, 50), 135, y + 13, p);
                }
                p.setTextSize(8);
                c.drawText(String.format(java.util.Locale.US, "%.2f", it.optDouble("cantidad", 0)), 410, y, p);
                c.drawText(String.format(java.util.Locale.US, "%.2f", it.optDouble("total", 0)), 464, y, p);
                c.drawText(String.format(java.util.Locale.US, "%.2f", it.optDouble("precio_unitario", 0)), 518, y, p);
                y += 24;
                itemIndex++;
                printed++;
            }

            if (itemIndex >= detail.length()) {
                int totalsY = 695;
                bold.setTextSize(10);
                p.setTextSize(10);
                c.drawText("SUBTOTAL", 395, totalsY, bold);
                c.drawText(money(doc.optDouble("subtotal", doc.optDouble("total", 0))), 485, totalsY, p);
                c.drawText("IGV", 395, totalsY + 22, bold);
                c.drawText(money(doc.optDouble("igv", 0)), 485, totalsY + 22, p);
                c.drawText("TOTAL", 395, totalsY + 44, bold);
                c.drawText(money(doc.optDouble("total", 0)), 485, totalsY + 44, bold);
                p.setTextSize(8);
                c.drawText("Estado pago: " + doc.optString("estado_pago", "PAGADO") + " / " + doc.optString("metodo_pago", ""), 34, 704, p);
                c.drawText("Garantia: conservar comprobante, series y accesorios del producto.", 34, 750, p);
                c.drawText("No hay garantia por daño fisico, sulfato, quemado o mala manipulacion.", 34, 765, p);
            } else {
                bold.setTextSize(9);
                c.drawText("CONTINUA EN PAGINA " + (pageNo + 1), 220, 705, bold);
            }
            p.setTextSize(8);
            c.drawText("Pagina " + pageNo, 510, 812, p);
            pdf.finishPage(page);
            pageNo++;
        } while (itemIndex < detail.length());

        try (FileOutputStream fos = new FileOutputStream(out)) {
            pdf.writeTo(fos);
        }
        pdf.close();
        return out;
    }

    private void openPdfFile(File file) {
        try {
            Uri uri = Uri.fromFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            startActivity(Intent.createChooser(intent, "Abrir PDF"));
        } catch (Exception e) {
            msg("PDF generado", "PDF creado en cache, pero no hay visor PDF instalado.");
        }
    }

    private String shortText(String value, int max) {
        if (value == null) return "";
        value = value.replace("\n", " ").trim();
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }

    private String safeFile(String value) {
        value = value == null ? "documento" : value;
        return value.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private void showClients() {
        runBusy("Cargando clientes...", () -> requestArray("/clientes", params()), arr -> {
            clearContent();
            content.addView(sectionTitle("Clientes", "Consulta rapida de documentos y datos"));
            EditText search = input("Buscar cliente", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            Runnable paint = () -> {
                list.removeAllViews();
                String q = search.getText().toString().toLowerCase();
                for (int i = 0; i < arr.length() && i < 150; i++) {
                    JSONObject c = arr.optJSONObject(i);
                    String txt = (c.optString("nombre") + " " + c.optString("numero_documento")).toLowerCase();
                    if (!q.isEmpty() && !txt.contains(q)) continue;
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(74));
                    lp.setMargins(0, 0, 0, dp(7));
                    list.addView(cardLabel(c.optString("nombre"), c.optString("tipo_documento") + ": " + c.optString("numero_documento") + " · " + c.optString("direccion"), TEAL), lp);
                }
            };
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            paint.run();
        });
    }

    private void showInventory() {
        loadProducts(arr -> {
            clearContent();
            content.addView(sectionTitle("Inventario", "Stock, busqueda y productos criticos"));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            for (int i = 0; i < productos.length() && i < 160; i++) {
                JSONObject p = productos.optJSONObject(i);
                int stock = p.optInt("stock");
                int bg = stock <= 0 ? Color.rgb(254, 226, 226) : stock <= 5 ? Color.rgb(254, 249, 195) : Color.WHITE;
                TextView row = cardLabel(p.optString("nombre"), p.optString("categoria") + " · Stock sistema: " + stock + " · " + money(p.optDouble("precio_venta")), TEAL);
                row.setBackground(rounded(bg, dp(12), Color.rgb(226, 232, 240)));
                row.setOnClickListener(v -> showProductSeries(p));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(78));
                lp.setMargins(0, 0, 0, dp(7));
                list.addView(row, lp);
            }
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        });
    }

    private void showInventoryPc() {
        loadProducts(arr -> {
            clearContent();
            selectedInventoryProduct = null;
            content.addView(sectionTitle("Inventario / Control por categoria", "Elige categoria para conteo. Toca un producto para seleccionarlo; manten presionado para ver series."));

            LinearLayout filter = new LinearLayout(this);
            filter.setOrientation(LinearLayout.VERTICAL);
            filter.setPadding(dp(10), dp(8), dp(10), dp(8));
            filter.setBackground(rounded(Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
            Spinner category = new Spinner(this);
            category.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"TODOS", "PROCESADOR", "PLACA MADRE", "ALMACENAMIENTO", "TARJ. GRAFICA", "CASE", "MONITOR", "FUENTE DE PODER", "MEMORIA RAM", "PERIFERICOS"}));
            EditText search = compactInput("Buscar");
            filter.addView(fieldRow(category, search));
            LinearLayout btns = new LinearLayout(this);
            btns.setOrientation(LinearLayout.HORIZONTAL);
            Button load = button("Cargar categoria", BLUE);
            Button count = button("Registrar conteo", PURPLE);
            Button adjust = button("Aplicar ajuste", GREEN);
            Button transfer = button("Transferir sucursal", ORANGE);
            btns.addView(load, new LinearLayout.LayoutParams(0, dp(44), 1));
            btns.addView(count, new LinearLayout.LayoutParams(0, dp(44), 1));
            btns.addView(adjust, new LinearLayout.LayoutParams(0, dp(44), 1));
            btns.addView(transfer, new LinearLayout.LayoutParams(0, dp(44), 1));
            filter.addView(btns);
            content.addView(filter);

            inventorySummary = label("Stock sistema cargado: 0 | Conteo fisico: sin datos", 14, TEXT, true);
            content.addView(inventorySummary);

            LinearLayout seriesBox = new LinearLayout(this);
            seriesBox.setOrientation(LinearLayout.VERTICAL);
            seriesBox.setPadding(dp(10), dp(8), dp(10), dp(8));
            seriesBox.setBackground(rounded(Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
            Spinner productCombo = new Spinner(this);
            fillProductSpinner(productCombo);
            EditText serie = compactInput("Serie");
            EditText proveedor = compactInput("Proveedor");
            EditText fecha = compactInput("YYYY-MM-DD");
            Spinner estado = new Spinner(this);
            estado.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"DISPONIBLE", "RESERVADO", "VENDIDO", "GARANTIA"}));
            seriesBox.addView(fieldRow(productCombo, serie, proveedor));
            seriesBox.addView(fieldRow(fecha, estado));
            LinearLayout serieBtns = new LinearLayout(this);
            serieBtns.setOrientation(LinearLayout.HORIZONTAL);
            Button saveSerie = button("Guardar serie", TEAL);
            Button viewSerie = button("Ver series del producto", NAVY_2);
            serieBtns.addView(saveSerie, new LinearLayout.LayoutParams(0, dp(44), 1));
            serieBtns.addView(viewSerie, new LinearLayout.LayoutParams(0, dp(44), 1));
            seriesBox.addView(serieBtns);
            content.addView(seriesBox);

            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

            Runnable paint = () -> paintInventoryTable(list, String.valueOf(category.getSelectedItem()), search.getText().toString());
            load.setOnClickListener(v -> paint.run());
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { paint.run(); }
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            count.setOnClickListener(v -> {
                if (selectedInventoryProduct == null) msg("Inventario", "Selecciona un producto.");
                else showCountDialog(selectedInventoryProduct, paint);
            });
            adjust.setOnClickListener(v -> applyInventoryCounts(paint));
            transfer.setOnClickListener(v -> {
                if (selectedInventoryProduct == null) msg("Inventario", "Selecciona un producto.");
                else showAdjustStockDialog(selectedInventoryProduct);
            });
            saveSerie.setOnClickListener(v -> {
                JSONObject p = productFromSpinner(productCombo);
                if (p == null) msg("Serie", "Selecciona producto.");
                else saveSerieForProduct(p, serie.getText().toString(), proveedor.getText().toString(), fecha.getText().toString(), String.valueOf(estado.getSelectedItem()));
            });
            viewSerie.setOnClickListener(v -> {
                JSONObject p = productFromSpinner(productCombo);
                if (p != null) showProductSeries(p);
            });
            paint.run();
        });
    }

    private void paintInventoryTable(LinearLayout list, String category, String q) {
        list.removeAllViews();
        GridLayout header = new GridLayout(this);
        header.setColumnCount(7);
        for (String h : new String[]{"ID", "Producto", "Categoria", "Marca", "Modelo", "Sistema", "Conteo/Dif."}) {
            header.addView(tableHeader(h), new ViewGroup.LayoutParams(getResources().getDisplayMetrics().widthPixels / 7, dp(34)));
        }
        list.addView(header);
        int totalSystem = 0, totalCount = 0, counted = 0;
        q = q.toLowerCase();
        for (int i = 0; i < productos.length() && i < 220; i++) {
            JSONObject p = productos.optJSONObject(i);
            String cat = p.optString("categoria");
            if (!"TODOS".equalsIgnoreCase(category) && !cat.equalsIgnoreCase(category)) continue;
            String text = (p.optString("nombre") + " " + cat + " " + p.optString("marca") + " " + p.optString("modelo")).toLowerCase();
            if (!q.isEmpty() && !text.contains(q)) continue;
            int stock = p.optInt("stock");
            totalSystem += stock;
            int pid = p.optInt("id");
            boolean hasCount = inventoryCounts.containsKey(pid);
            int count = hasCount ? inventoryCounts.get(pid) : 0;
            if (hasCount) { counted++; totalCount += count; }
            int diff = hasCount ? count - stock : 0;
            TextView row = cardLabel(pid + " | " + p.optString("nombre"), cat + " | " + p.optString("marca") + " | " + p.optString("modelo") + " | Sistema: " + stock + " | Conteo: " + (hasCount ? count : "-") + " | Dif: " + (hasCount ? diff : "-"), TEAL);
            int bg = hasCount ? (diff == 0 ? Color.rgb(220, 252, 231) : diff > 0 ? Color.rgb(219, 234, 254) : Color.rgb(254, 226, 226)) : (stock <= 0 ? Color.rgb(254, 226, 226) : stock <= 5 ? Color.rgb(254, 249, 195) : Color.WHITE);
            row.setBackground(rounded(bg, dp(8), Color.rgb(226, 232, 240)));
            row.setOnClickListener(v -> selectedInventoryProduct = p);
            row.setOnLongClickListener(v -> { selectedInventoryProduct = p; showProductSeries(p); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(66));
            lp.setMargins(0, 0, 0, dp(4));
            list.addView(row, lp);
        }
        if (inventorySummary != null) {
            if (counted == 0) inventorySummary.setText("Stock sistema cargado: " + totalSystem + " | Conteo fisico: sin datos");
            else inventorySummary.setText("Conteo fisico: " + totalCount + " | Sistema: " + totalSystem + " | Diferencia: " + (totalCount - totalSystem));
        }
    }

    private void fillProductSpinner(Spinner spinner) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < productos.length(); i++) {
            JSONObject p = productos.optJSONObject(i);
            values.add(p.optInt("id") + " - " + p.optString("nombre"));
        }
        spinner.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, values));
    }

    private JSONObject productFromSpinner(Spinner spinner) {
        String value = spinner == null || spinner.getSelectedItem() == null ? "" : String.valueOf(spinner.getSelectedItem());
        int id = parseInt(value.split(" - ")[0]);
        for (int i = 0; i < productos.length(); i++) {
            JSONObject p = productos.optJSONObject(i);
            if (p.optInt("id") == id) return p;
        }
        return null;
    }

    private void showCountDialog(JSONObject product, Runnable repaint) {
        EditText count = compactInput("Cantidad fisica");
        new AlertDialog.Builder(this)
                .setTitle("Conteo fisico: " + product.optString("nombre"))
                .setView(count)
                .setPositiveButton("Guardar conteo", (d, w) -> {
                    inventoryCounts.put(product.optInt("id"), parseInt(count.getText().toString()));
                    repaint.run();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void applyInventoryCounts(Runnable repaint) {
        if (inventoryCounts.isEmpty()) {
            msg("Inventario", "No hay conteos registrados.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Aplicar ajuste")
                .setMessage("¿Actualizar stock real con el conteo fisico registrado?")
                .setPositiveButton("Aplicar", (d, w) -> runBusy("Aplicando ajustes...", () -> {
                    int okCount = 0;
                    for (Map.Entry<Integer, Integer> e : inventoryCounts.entrySet()) {
                        JSONObject payload = new JSONObject();
                        payload.put("stock", e.getValue());
                        JSONObject r = request("POST", "/productos/" + e.getKey() + "/ajustar-stock", params(), payload);
                        if (ok(r)) okCount++;
                    }
                    JSONObject out = new JSONObject();
                    out.put("ok", true);
                    out.put("count", okCount);
                    return out;
                }, r -> {
                    productos = new JSONArray();
                    productosCacheMs = 0;
                    inventoryCounts.clear();
                    msg("Inventario", "Productos ajustados: " + r.optInt("count"));
                    showInventoryPc();
                }))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void saveSerieForProduct(JSONObject product, String serie, String proveedor, String fecha, String estado) {
        String sn = serie == null ? "" : serie.trim();
        if (sn.isEmpty()) {
            msg("Serie", "Ingresa la serie.");
            return;
        }
        runBusy("Guardando serie...", () -> {
            JSONObject payload = new JSONObject();
            payload.put("producto_id", product.optInt("id"));
            payload.put("serie", sn);
            payload.put("proveedor", proveedor == null ? "" : proveedor.trim());
            payload.put("fecha_ingreso", fecha == null ? "" : fecha.trim());
            payload.put("estado", estado == null ? "DISPONIBLE" : estado);
            payload.put("fecha_salida", JSONObject.NULL);
            payload.put("sucursal", sucursal);
            return request("POST", "/series", null, payload);
        }, r -> {
            if (ok(r)) {
                productos = new JSONArray();
                productosCacheMs = 0;
                msg("Serie", "Serie guardada correctamente.");
                showInventoryPc();
            } else msg("Serie", r.optString("msg", "No se pudo guardar la serie."));
        });
    }

    private void showProductSeries(JSONObject product) {
        runBusy("Cargando series...", () -> requestArray("/series", params()), arr -> {
            LinearLayout box = new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(10), dp(8), dp(10), dp(8));
            box.addView(label(product.optString("nombre"), 16, TEXT, true));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            scroll.addView(list);
            int count = 0;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.optJSONObject(i);
                if (s.optInt("producto_id") != product.optInt("id")) continue;
                count++;
                list.addView(cardLabel(s.optString("serie"), s.optString("estado") + " / " + s.optString("proveedor") + " / " + s.optString("fecha_ingreso"), TEAL), new LinearLayout.LayoutParams(-1, dp(72)));
            }
            if (count == 0) list.addView(label("Sin series registradas.", 14, MUTED, false));
            box.addView(scroll, new LinearLayout.LayoutParams(-1, dp(330)));
            new AlertDialog.Builder(this)
                    .setTitle("Series del producto")
                    .setView(box)
                    .setPositiveButton("Agregar serie", (d, w) -> showAddSeriesDialog(product))
                    .setNegativeButton("Cerrar", null)
                    .show();
        });
    }

    private void showAddSeriesDialog(JSONObject product) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        EditText serie = input("Serie unica", false);
        EditText proveedor = input("Proveedor", false);
        EditText fecha = input("Fecha ingreso YYYY-MM-DD", false);
        Spinner estado = new Spinner(this);
        estado.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"DISPONIBLE", "RESERVADO", "VENDIDO", "GARANTIA"}));
        box.addView(label(product.optString("nombre"), 14, TEXT, true));
        box.addView(serie);
        box.addView(proveedor);
        box.addView(fecha);
        box.addView(estado);
        new AlertDialog.Builder(this)
                .setTitle("Agregar serie")
                .setView(box)
                .setPositiveButton("Guardar", (d, w) -> {
                    String sn = serie.getText().toString().trim();
                    if (sn.isEmpty()) {
                        msg("Serie", "Ingresa la serie.");
                        return;
                    }
                    runBusy("Guardando serie...", () -> {
                        JSONObject payload = new JSONObject();
                        payload.put("producto_id", product.optInt("id"));
                        payload.put("serie", sn);
                        payload.put("proveedor", proveedor.getText().toString().trim());
                        payload.put("fecha_ingreso", fecha.getText().toString().trim());
                        payload.put("estado", String.valueOf(estado.getSelectedItem()));
                        payload.put("fecha_salida", JSONObject.NULL);
                        payload.put("sucursal", sucursal);
                        return request("POST", "/series", null, payload);
                    }, r -> {
                        if (ok(r)) {
                            msg("Serie", "Serie guardada correctamente.");
                            productos = new JSONArray();
                            productosCacheMs = 0;
                            showInventory();
                        } else {
                            msg("Serie", r.optString("msg", "No se pudo guardar la serie."));
                        }
                    });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAdjustStockDialog(JSONObject product) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        EditText stock = input("Nuevo stock", false);
        stock.setText(String.valueOf(product.optInt("stock", 0)));
        EditText destino = input("Sucursal destino para transferir", false);
        EditText cantidad = input("Cantidad a transferir", false);
        box.addView(label(product.optString("nombre"), 14, TEXT, true));
        box.addView(stock);
        box.addView(label("Transferencia opcional", 13, MUTED, true));
        box.addView(destino);
        box.addView(cantidad);
        new AlertDialog.Builder(this)
                .setTitle("Stock / Transferencia")
                .setView(box)
                .setPositiveButton("Ajustar stock", (d, w) -> runBusy("Ajustando stock...", () -> {
                    JSONObject payload = new JSONObject();
                    payload.put("stock", parseInt(stock.getText().toString()));
                    return request("POST", "/productos/" + product.optInt("id") + "/ajustar-stock", params(), payload);
                }, r -> {
                    if (ok(r)) {
                        productos = new JSONArray();
                        productosCacheMs = 0;
                        msg("Inventario", "Stock actualizado.");
                        showInventory();
                    } else msg("Inventario", r.optString("msg", "No se pudo ajustar."));
                }))
                .setNegativeButton("Transferir", (d, w) -> {
                    String dst = destino.getText().toString().trim();
                    int qty = parseInt(cantidad.getText().toString());
                    if (dst.isEmpty() || qty <= 0) {
                        msg("Transferencia", "Ingresa sucursal destino y cantidad.");
                        return;
                    }
                    runBusy("Transfiriendo stock...", () -> {
                        JSONObject payload = new JSONObject();
                        payload.put("producto_id", product.optInt("id"));
                        payload.put("cantidad", qty);
                        payload.put("sucursal_origen", sucursal);
                        payload.put("sucursal_destino", dst);
                        payload.put("usuario", usuario);
                        payload.put("nota", "Transferido desde Android");
                        return request("POST", "/stock/transferir", null, payload);
                    }, r -> {
                        if (ok(r)) {
                            productos = new JSONArray();
                            productosCacheMs = 0;
                            msg("Transferencia", "Stock transferido.");
                            showInventory();
                        } else msg("Transferencia", r.optString("msg", "No se pudo transferir."));
                    });
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void showGarantias() {
        runBusy("Cargando garantias...", () -> requestArray("/garantias", params()), arr -> {
            clearContent();
            content.addView(sectionTitle("Garantias", "Seguimiento de equipos, series, fallas y solucion"));
            LinearLayout form = new LinearLayout(this);
            form.setOrientation(LinearLayout.VERTICAL);
            form.setPadding(dp(10), dp(8), dp(10), dp(8));
            form.setBackground(rounded(Color.WHITE, dp(8), Color.rgb(226, 232, 240)));
            EditText cliente = compactInput("Cliente");
            EditText documento = compactInput("Documento");
            EditText producto = compactInput("Producto");
            EditText serie = compactInput("Serie");
            EditText falla = compactInput("Falla reportada");
            EditText solucion = compactInput("Solucion / diagnostico");
            Spinner estado = new Spinner(this);
            estado.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, new String[]{"RECIBIDO", "REVISION", "APROBADO", "RECHAZADO", "ENTREGADO"}));
            form.addView(fieldRow(cliente, documento, producto));
            form.addView(fieldRow(serie, estado));
            form.addView(fieldRow(falla, solucion));
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            Button nuevo = button("Nueva garantia", TEAL);
            Button limpiar = button("Limpiar", NAVY_2);
            actions.addView(nuevo, new LinearLayout.LayoutParams(0, dp(44), 1));
            actions.addView(limpiar, new LinearLayout.LayoutParams(0, dp(44), 1));
            form.addView(actions);
            content.addView(form, new LinearLayout.LayoutParams(-1, -2));
            EditText search = input("Buscar garantia", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            final JSONObject[] selected = new JSONObject[]{null};
            Runnable clearForm = () -> {
                selected[0] = null;
                cliente.setText(""); documento.setText(""); producto.setText(""); serie.setText(""); falla.setText(""); solucion.setText("");
                setSpinnerValue(estado, "RECIBIDO");
                nuevo.setText("Nueva garantia");
            };
            Runnable paint = () -> {
                list.removeAllViews();
                String q = search.getText().toString().toLowerCase();
                for (int i = 0; i < arr.length() && i < 200; i++) {
                    JSONObject g = arr.optJSONObject(i);
                    String rowText = (g.optString("cliente") + " " + g.optString("documento") + " " + g.optString("producto") + " " + g.optString("serie") + " " + g.optString("falla") + " " + g.optString("estado")).toLowerCase();
                    if (!q.isEmpty() && !rowText.contains(q)) continue;
                    TextView row = cardLabel(g.optString("producto"), g.optString("cliente") + " - " + g.optString("serie") + " - " + g.optString("estado") + "\n" + g.optString("falla"), PURPLE);
                    row.setOnClickListener(v -> {
                        selected[0] = g;
                        cliente.setText(g.optString("cliente"));
                        documento.setText(g.optString("documento"));
                        producto.setText(g.optString("producto"));
                        serie.setText(g.optString("serie"));
                        falla.setText(g.optString("falla"));
                        solucion.setText(g.optString("solucion"));
                        setSpinnerValue(estado, g.optString("estado", "RECIBIDO"));
                        nuevo.setText("Actualizar garantia");
                    });
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(92));
                    lp.setMargins(0, 0, 0, dp(7));
                    list.addView(row, lp);
                }
            };
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            limpiar.setOnClickListener(v -> clearForm.run());
            nuevo.setOnClickListener(v -> saveWarranty(selected[0], cliente, documento, producto, serie, falla, solucion, estado));
            paint.run();
        });
    }

    private void saveWarranty(JSONObject selected, EditText cliente, EditText documento, EditText producto, EditText serie, EditText falla, EditText solucion, Spinner estado) {
        if (cliente.getText().toString().trim().isEmpty() || producto.getText().toString().trim().isEmpty()) {
            msg("Garantias", "Ingresa cliente y producto.");
            return;
        }
        boolean edit = selected != null;
        runBusy(edit ? "Actualizando garantia..." : "Guardando garantia...", () -> {
            JSONObject payload = new JSONObject();
            payload.put("cliente", cliente.getText().toString().trim());
            payload.put("documento", documento.getText().toString().trim());
            payload.put("producto", producto.getText().toString().trim());
            payload.put("serie", serie.getText().toString().trim());
            payload.put("falla", falla.getText().toString().trim());
            payload.put("estado", String.valueOf(estado.getSelectedItem()));
            payload.put("solucion", solucion.getText().toString().trim());
            payload.put("usuario", usuario);
            payload.put("sucursal", sucursal);
            return request(edit ? "PUT" : "POST", edit ? "/garantias/" + selected.optInt("id") : "/garantias", edit ? params() : null, payload);
        }, r -> {
            if (ok(r)) {
                msg("Garantias", edit ? "Garantia actualizada." : "Garantia registrada.");
                showGarantias();
            } else {
                msg("Garantias", r.optString("msg", "No se pudo guardar."));
            }
        });
    }
    private void showUsers() {
        runBusy("Cargando usuarios...", () -> requestArray("/usuarios", params()), arr -> {
            clearContent();
            content.addView(sectionTitle("Usuarios", "Roles, sucursal y foto asignada"));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            for (int i = 0; i < arr.length() && i < 120; i++) {
                JSONObject u = arr.optJSONObject(i);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(76));
                lp.setMargins(0, 0, 0, dp(7));
                list.addView(cardLabel(u.optString("usuario"), u.optString("rol") + " · " + u.optString("sucursal"), BLUE), lp);
            }
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        });
    }

    private void showAudit() {
        runBusy("Cargando registro...", () -> requestArray("/auditoria", params()), arr -> {
            clearContent();
            content.addView(sectionTitle("Registro", "Movimientos de usuarios: ventas, borrados, cambios y accesos"));
            EditText search = input("Buscar usuario, accion o modulo", false);
            search.setBackground(rounded(Color.WHITE, dp(12), Color.rgb(203, 213, 225)));
            content.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
            Runnable paint = () -> {
                list.removeAllViews();
                String q = search.getText().toString().toLowerCase();
                for (int i = 0; i < arr.length() && i < 200; i++) {
                    JSONObject a = arr.optJSONObject(i);
                    String text = (a.optString("usuario") + " " + a.optString("accion") + " " + a.optString("modulo") + " " + a.optString("detalle")).toLowerCase();
                    if (!q.isEmpty() && !text.contains(q)) continue;
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(82));
                    lp.setMargins(0, 0, 0, dp(7));
                    list.addView(cardLabel(a.optString("usuario") + " - " + a.optString("accion"), a.optString("modulo") + " | " + a.optString("detalle") + " | " + a.optString("fecha"), TEAL), lp);
                }
            };
            search.addTextChangedListener(new SimpleTextWatcher(paint));
            paint.run();
        });
    }

    private void showSettings() {
        clearContent();
        content.addView(sectionTitle("Configuracion", "Sucursal, conexion API, autoupdate y herramientas"));
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(10), dp(10), dp(10));
        scroll.addView(box);

        box.addView(cardLabel("API Render", API, TEAL), new LinearLayout.LayoutParams(-1, dp(78)));
        box.addView(cardLabel("Sucursal actual", sucursal, BLUE), new LinearLayout.LayoutParams(-1, dp(78)));
        box.addView(cardLabel("Usuario", usuario + " - " + rol, PURPLE), new LinearLayout.LayoutParams(-1, dp(78)));

        Button testApi = button("Probar conexion API", GREEN);
        Button update = button("Buscar actualizacion APK", BLUE);
        Button reloadProducts = button("Recargar cache productos", ORANGE);
        Button openWeb = button("Abrir Pagina Web", PURPLE);
        testApi.setOnClickListener(v -> runBusy("Probando API...", () -> request("GET", "/", null, null), r -> msg("API", ok(r) ? "Conexion correcta con Render." : r.optString("msg", "No respondio la API."))));
        update.setOnClickListener(v -> checkAndroidUpdate(true));
        reloadProducts.setOnClickListener(v -> {
            productos = new JSONArray();
            productosCacheMs = 0;
            imageCache.clear();
            msg("Cache", "Cache limpiada. Al entrar a Ventas/Productos cargara desde la API.");
        });
        openWeb.setOnClickListener(v -> showWeb());
        for (Button b : new Button[]{testApi, update, reloadProducts, openWeb}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
            lp.setMargins(0, dp(5), 0, dp(5));
            box.addView(b, lp);
        }
        box.addView(label("Nota: las imagenes y productos se leen desde la API/Render. Para actualizar a todos, sube el APK al Release y cambia ANDROID_APP_VERSION.", 13, MUTED, false));
        content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    }

    private void showWeb() {
        runBusy("Cargando WooCommerce...", () -> request("GET", "/web/woocommerce/products", params(), null), r -> {
            clearContent();
            content.addView(sectionTitle("Pagina Web", "Productos WooCommerce para COMPUTER ARMY"));
            GridLayout actions = new GridLayout(this);
            actions.setColumnCount(2);
            Button test = button("Probar conexion", BLUE);
            Button importWeb = button("Cargar Web -> ERP", GREEN);
            Button syncWeb = button("ERP -> Web", PURPLE);
            Button syncImg = button("Imagenes Web -> ERP", ORANGE);
            test.setOnClickListener(v -> runBusy("Probando WooCommerce...", () -> request("GET", "/web/woocommerce/test", params(), null), res -> msg("WooCommerce", res.optString("msg", ok(res) ? "Conexion correcta." : "No se pudo conectar."))));
            importWeb.setOnClickListener(v -> runBusy("Importando productos web...", () -> request("POST", "/web/woocommerce/import-products", params(), new JSONObject()), res -> {
                msg("WooCommerce", ok(res) ? "Productos importados. Creados: " + res.optInt("created") + " / Actualizados: " + res.optInt("updated") : res.optString("msg", "No se pudo importar."));
                productos = new JSONArray();
                productosCacheMs = 0;
                showWeb();
            }));
            syncWeb.setOnClickListener(v -> runBusy("Sincronizando ERP hacia web...", () -> request("POST", "/web/woocommerce/sync-products", params(), new JSONObject()), res -> msg("WooCommerce", ok(res) ? "Sincronizados: " + res.optInt("sync_ok") + " de " + res.optInt("total") : res.optString("msg", "No se pudo sincronizar."))));
            syncImg.setOnClickListener(v -> runBusy("Sincronizando imagenes...", () -> request("POST", "/web/woocommerce/sync-images", params(), null), res -> {
                msg("WooCommerce", ok(res) ? "Imagenes actualizadas: " + res.optInt("updated") : res.optString("msg", "No se pudo sincronizar imagenes."));
                productos = new JSONArray();
                productosCacheMs = 0;
            }));
            for (Button b : new Button[]{test, importWeb, syncWeb, syncImg}) {
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = getResources().getDisplayMetrics().widthPixels / 2 - dp(12);
                lp.height = dp(48);
                lp.setMargins(dp(4), dp(4), dp(4), dp(4));
                actions.addView(b, lp);
            }
            content.addView(actions);
            ScrollView scroll = new ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(list);
            JSONArray arr = r.optJSONArray("data");
            if (arr == null) arr = new JSONArray();
            for (int i = 0; i < arr.length() && i < 100; i++) {
                JSONObject p = arr.optJSONObject(i);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(82));
                lp.setMargins(0, 0, 0, dp(7));
                list.addView(cardLabel(p.optString("name"), "SKU: " + p.optString("sku") + " · Stock: " + p.optString("stock_quantity") + " · S/ " + p.optString("price"), GREEN), lp);
            }
            if (arr.length() == 0) {
                list.addView(cardLabel("WooCommerce", r.optString("msg", "Sin productos o configuracion pendiente."), ORANGE), new LinearLayout.LayoutParams(-1, dp(90)));
            }
            content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        });
    }

    private JSONObject params() {
        JSONObject p = new JSONObject();
        try {
            p.put("sucursal", sucursal);
            p.put("empresa", sucursal);
        } catch (Exception ignored) {}
        return p;
    }

    private JSONObject singleParam(String k, String v) {
        JSONObject p = new JSONObject();
        try { p.put(k, v); } catch (Exception ignored) {}
        return p;
    }

    private boolean ok(JSONObject r) {
        return r != null && (r.optBoolean("ok") || r.optBoolean("success") || r.has("data"));
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt((value == null ? "0" : value.trim()).replace(",", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble((value == null ? "0" : value.trim()).replace(",", "."));
        } catch (Exception e) {
            return 0;
        }
    }

    private void setSpinnerValue(Spinner spinner, String value) {
        if (spinner == null || value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (String.valueOf(spinner.getItemAtPosition(i)).equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    private String money(double value) {
        return String.format(java.util.Locale.US, "S/ %,.2f", value);
    }

    private void msg(String title, String body) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK", null).show();
    }

    private interface JsonWork { JSONObject run() throws Exception; }
    private interface JsonDone { void done(JSONObject r); }
    private interface ArrayWork { JSONArray run() throws Exception; }
    private interface ArrayDone { void done(JSONArray r); }
    private interface ProductDone { void done(JSONArray r); }

    private void runBusy(String text, JsonWork work, JsonDone done) {
        ProgressDialog d = ProgressDialog.show(this, "", text, true, false);
        new Thread(() -> {
            JSONObject r;
            try { r = work.run(); } catch (Exception e) { r = new JSONObject(); try { r.put("ok", false); r.put("msg", e.toString()); } catch (Exception ignored) {} }
            JSONObject finalR = r;
            runOnUiThread(() -> { d.dismiss(); done.done(finalR); });
        }).start();
    }

    private void runBusy(String text, ArrayWork work, ArrayDone done) {
        ProgressDialog d = ProgressDialog.show(this, "", text, true, false);
        new Thread(() -> {
            JSONArray r;
            try { r = work.run(); } catch (Exception e) { r = new JSONArray(); }
            JSONArray finalR = r;
            runOnUiThread(() -> { d.dismiss(); done.done(finalR); });
        }).start();
    }

    private JSONObject request(String method, String path, JSONObject params, JSONObject payload) throws Exception {
        StringBuilder url = new StringBuilder(API).append(path);
        if (params != null && params.length() > 0) {
            url.append("?");
            boolean first = true;
            java.util.Iterator<String> keys = params.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                if (!first) url.append("&");
                first = false;
                url.append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(params.optString(k), "UTF-8"));
            }
        }
        HttpURLConnection c = (HttpURLConnection) new URL(url.toString()).openConnection();
        c.setRequestMethod(method);
        c.setConnectTimeout(25000);
        c.setReadTimeout(25000);
        c.setRequestProperty("Accept", "application/json");
        if (payload != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            try (OutputStream os = c.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getResponseCode() >= 400 ? c.getErrorStream() : c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) out.append(line);
        return out.length() == 0 ? new JSONObject() : new JSONObject(out.toString());
    }

    private JSONArray requestArray(String path, JSONObject params) throws Exception {
        StringBuilder url = new StringBuilder(API).append(path).append("?");
        java.util.Iterator<String> keys = params.keys();
        boolean first = true;
        while (keys.hasNext()) {
            String k = keys.next();
            if (!first) url.append("&");
            first = false;
            url.append(URLEncoder.encode(k, "UTF-8")).append("=").append(URLEncoder.encode(params.optString(k), "UTF-8"));
        }
        HttpURLConnection c = (HttpURLConnection) new URL(url.toString()).openConnection();
        c.setConnectTimeout(25000);
        c.setReadTimeout(25000);
        BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) out.append(line);
        return out.length() == 0 ? new JSONArray() : new JSONArray(out.toString());
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable r;
        SimpleTextWatcher(Runnable r) { this.r = r; }
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        public void onTextChanged(CharSequence s, int start, int before, int count) { r.run(); }
        public void afterTextChanged(android.text.Editable s) {}
    }
}
