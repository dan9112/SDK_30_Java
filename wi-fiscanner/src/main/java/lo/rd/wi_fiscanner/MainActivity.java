package lo.rd.wi_fiscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.ACCESS_WIFI_STATE;

public class MainActivity extends AppCompatActivity {

    /**
     * Тег логов
     */
    private static final String LOG_TAG = "AndroidExample";

    /**
     * Код запроса проверки разрешений
     */
    private static final int MY_REQUEST_CODE = 123;
    /**
     * Кнопка проверки состояния Wi-fi модуля
     */
    Button buttonState = findViewById(R.id.button_state);
    /**
     * Кнопка запуска сканирования всех доступных устройств
     */
    Button buttonScan = findViewById(R.id.button_scan);
    /**
     * Экземпляр класса для управления всеми аспектами подключения Wi-Fi.
     *
     * @see WifiManager
     */
    private WifiManager wifiManager;
    /**
     * Окно для ввода пароля доступа к подключаемой точке доступа
     */
    private EditText editTextPassword;
    /**
     * Layout со списком доступных для подключения Wi-fi сетей в виде кнопок.
     * По нажатию на соответствующую кнопку происходит попытка подключения к указанной на кнопке
     * точке доступа с введённым в {@link #editTextPassword} паролем
     *
     * @see #connectToNetwork(String, String)
     * @see #showNetworks(List)
     */
    private LinearLayout linearLayoutScanResults;
    /**
     * Текстовое окно с перечислением всех Wi-fi сетей, полученных в результате сканирования, и
     * подробной информацией о них
     *
     * @see #showNetworksDetails(List)
     */
    private TextView textViewScanResults;
    /**
     * Приёмник широковещательных сообщений. Нужен для фильтрации
     */
    private WifiBroadcastReceiver wifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Инициализация приёмника
        wifiReceiver = new WifiBroadcastReceiver();

        // Регистрация приёмника
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        editTextPassword = findViewById(R.id.editText_password);
        textViewScanResults = findViewById(R.id.textView_scanResults);
        linearLayoutScanResults = findViewById(R.id.linearLayout_scanResults);

        // Регистрация слушателей нажатия на кнопки
        buttonState.setOnClickListener(view -> showWifiState());

        buttonScan.setOnClickListener(view -> askAndStartScanWifi());
    }

    /**
     * Функция проверки разрешений и начала сканирования. Будет запрашивать у пользователя доступ к
     * необходимым для функционирования приложения службам
     */
    private void askAndStartScanWifi() {

        // With Android Level >= 23, you have to ask the user
        // for permission to Call.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) { // 23
            int permission1 =
                    ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION);

            // Check for permissions
            if (permission1 != PackageManager.PERMISSION_GRANTED) {

                Log.d(LOG_TAG, "Requesting Permissions");

                // Request permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                ACCESS_COARSE_LOCATION,
                                ACCESS_FINE_LOCATION,
                                ACCESS_WIFI_STATE,
                                ACCESS_NETWORK_STATE
                        }, MY_REQUEST_CODE);
                return;
            }
            Log.d(LOG_TAG, "Permissions Already Granted");
        }
        doStartScanWifi();
    }

    /**
     * Запуск сканирования устройств
     */
    private void doStartScanWifi() {
        wifiManager.startScan();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult");

        if (requestCode == MY_REQUEST_CODE) {// Если запрос отменен, массивы результатов пусты
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.d(LOG_TAG, "Permission Granted: " + permissions[0]);

                doStartScanWifi();
            } else {
                // В разрешении отказано, бу!Отключите функции,
                // зависящие от этого разрешения.
                Log.d(LOG_TAG, "Permission Denied: " + permissions[0]);
            }
        }
    }

    /**
     * Функция вывода статуса Wi-fi модуля
     */
    private void showWifiState() {
        int state = wifiManager.getWifiState();
        String statusInfo;

        switch (state) {
            case WifiManager.WIFI_STATE_DISABLING:
                statusInfo = "Disabling";
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                statusInfo = "Disabled";
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                statusInfo = "Enabling";
                break;
            case WifiManager.WIFI_STATE_ENABLED:
                statusInfo = "Enabled";
                break;
            default:
                statusInfo = "Unknown";
                break;
        }
        Toast.makeText(this, "Wi-fi Status: " + statusInfo, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(wifiReceiver);
        super.onStop();
    }

    /**
     * Функция вывода добавления кнопок для доступа к сетям Wi-fi.
     * На каждой кнопке указанна краткая информация о каждой из полученных в результате сканирования
     * сетей
     *
     * @param results список полученыых в результате сканирования сетей
     * @see #askAndStartScanWifi()
     */
    private void showNetworks(List<ScanResult> results) {
        // удаление всех виджетов из layout
        linearLayoutScanResults.removeAllViews();

        // цикл создания кнопок для подключения
        for (final ScanResult result : results) {
            final String networkCapabilities = result.capabilities;
            final String networkSSID = result.SSID; // Network Name.
            Button button = new Button(this);

            button.setText(String.format("%s (%s)", networkSSID, networkCapabilities));
            linearLayoutScanResults.addView(button);

            // добавление слушателя нажатия на кнопку
            button.setOnClickListener(view -> {
                String networkCapabilities1 = result.capabilities;
                connectToNetwork(networkCapabilities1, networkSSID);
            });
        }
    }

    /**
     * Функция вывода сетей wi-fi в {@link #textViewScanResults} с подробной информацией о каждой
     *
     * @param results список полученыых в результате сканирования сетей
     */
    private void showNetworksDetails(List<ScanResult> results) {

        textViewScanResults.setText("");
        StringBuilder sb = new StringBuilder();
        sb.append("Networks count: ").append(results.size());

        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            sb.append("\n\n  --------- Network ").append(i + 1).append("/").append(results.size()).append(" ---------");

            sb.append("\n capabilities: ").append(result.capabilities);
            sb.append("\n SSID: ").append(result.SSID); // Network Name.

            sb.append("\n BSSID: ").append(result.BSSID);
            sb.append("\n frequency: ").append(result.frequency);
            sb.append("\n level: ").append(result.level);

            sb.append("\n describeContents(): ").append(result.describeContents());

            // Level >17, Android 4.2
            sb.append("\n timestamp: ").append(result.timestamp);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Level 23, Android 6.0
                sb.append("\n centerFreq0: ").append(result.centerFreq0);
                sb.append("\n centerFreq1: ").append(result.centerFreq1);
                sb.append("\n venueName: ").append(result.venueName);
                sb.append("\n operatorFriendlyName: ").append(result.operatorFriendlyName);
                sb.append("\n channelWidth: ").append(result.channelWidth);
                sb.append("\n is80211mcResponder(): ").append(result.is80211mcResponder());
                sb.append("\n isPasspointNetwork(): ").append(result.isPasspointNetwork());
            }
        }
        textViewScanResults.setText(sb.toString());
    }

    /**
     * Функция попытки подключения к выбранной точке доступа wi-fi
     *
     * @param networkCapabilities режим безопасности выбранной точки доступа
     * @param networkSSID         наименование выбранной точки доступа
     */
    private void connectToNetwork(String networkCapabilities, String networkSSID) {
        Toast.makeText(this, "Connecting to network: " + networkSSID, Toast.LENGTH_SHORT).show();

        String networkPass = editTextPassword.getText().toString();

        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = "\"" + networkSSID + "\"";

        if (networkCapabilities.toUpperCase().contains("WEP")) { // WEP Network.
            Toast.makeText(this, "WEP Network", Toast.LENGTH_SHORT).show();

            wifiConfig.wepKeys[0] = "\"" + networkPass + "\"";
            wifiConfig.wepTxKeyIndex = 0;
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
        } else if (networkCapabilities.toUpperCase().contains("WPA")) { // WPA Network
            Toast.makeText(this, "WPA Network", Toast.LENGTH_SHORT).show();
            wifiConfig.preSharedKey = "\"" + networkPass + "\"";
        } else { // OPEN Network.
            Toast.makeText(this, "OPEN Network", Toast.LENGTH_SHORT).show();
            wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }

        wifiManager.addNetwork(wifiConfig);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration config : list) {
            if (config.SSID != null && config.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(config.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
    }

    /**
     * Класс для прослушивания широковещательных сообщений
     */
    class WifiBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive()");

            Toast.makeText(MainActivity.this, "Scan Complete!", Toast.LENGTH_SHORT).show();

            boolean ok = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);

            if (ok) {
                Log.d(LOG_TAG, "Scan OK");

                List<ScanResult> list = wifiManager.getScanResults();

                showNetworks(list);
                showNetworksDetails(list);
            } else {
                Log.d(LOG_TAG, "Scan not OK");
            }
        }
    }
}
