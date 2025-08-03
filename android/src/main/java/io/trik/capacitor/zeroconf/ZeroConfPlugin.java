package io.trik.capacitor.zeroconf;

import android.Manifest;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import java.net.InetAddress;
import java.util.Map;

@CapacitorPlugin(
    name = "ZeroConf",
    permissions = {
        @Permission(
            strings = {
                Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_MULTICAST_STATE, Manifest.permission.INTERNET
            },
            alias = "internet"
        )
    }
)
public class ZeroConfPlugin extends Plugin {

    private static final String TAG = "ZeroConf";

    private final ZeroConf implementation = new ZeroConf();

    @Override
    public void load() {
        implementation.initialize(getActivity());
    }

    @PluginMethod
    public void getHostname(PluginCall call) {
        String hostname = implementation.getHostname();
        if (hostname != null) {
            JSObject result = new JSObject();
            result.put("hostname", hostname);
            call.resolve(result);
        } else {
            call.reject("Error: undefined hostname");
        }
    }

    @PluginMethod
    public void register(PluginCall call) {
        final String type = call.getString("type");
        final String domain = call.getString("domain");
        final String name = call.getString("name");
        final int port = call.getInt("port");
        final JSObject props = call.getObject("props");
        final String addressFamily = call.getString("addressFamily");

        getBridge()
            .executeOnMainThread(() -> {
                try {
                    NsdServiceInfo service = implementation.registerService(type, domain, name, port, props, addressFamily);
                    JSObject status = new JSObject();
                    status.put("action", "registered");
                    status.put("service", jsonifyService(service));

                    call.resolve(status);
                } catch (RuntimeException e) {
                    call.reject(e.getMessage());
                }
            });
    }

    @PluginMethod
    public void unregister(PluginCall call) {
        final String type = call.getString("type");
        final String domain = call.getString("domain");
        final String name = call.getString("name");

        getBridge()
            .executeOnMainThread(() -> {
                implementation.unregisterService(type, domain, name);
                call.resolve();
            });
    }

    @PluginMethod
    public void stop(PluginCall call) {
        getBridge()
            .executeOnMainThread(() -> {
                implementation.stop();
                call.resolve();
            });
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void watch(PluginCall call) {
        final String type = call.getString("type");
        final String domain = call.getString("domain");
        final String addressFamily = call.getString("addressFamily");

        getBridge()
            .executeOnMainThread(() -> {
                try {
                    implementation.watchService(
                        type,
                        domain,
                        addressFamily,
                        (action, service) -> {
                            JSObject status = new JSObject();
                            status.put("action", action);
                            status.put("service", jsonifyService(service));

                            call.setKeepAlive(true);
                            call.resolve(status);
                        }
                    );
                } catch (RuntimeException e) {
                    call.reject("Error: " + e.getMessage());
                }
            });

        call.setKeepAlive(true);
        call.resolve();
    }

    @PluginMethod
    public void unwatch(PluginCall call) {
        final String type = call.getString("type");
        final String domain = call.getString("domain");

        getBridge()
            .executeOnMainThread(() -> {
                implementation.unwatchService(type, domain);
                call.resolve();
            });
    }

    @PluginMethod
    public void close(PluginCall call) {
        getBridge()
            .executeOnMainThread(() -> {
                implementation.close();
                call.resolve();
            });
    }

    private static JSObject jsonifyService(NsdServiceInfo service) {
        JSObject obj = new JSObject();

        // Extract domain from service type (NSD doesn't separate domain like JmDNS)
        String serviceType = service.getServiceType();
        String domain = "local."; // Default domain for mDNS
        String type = serviceType;
        
        obj.put("domain", domain);
        obj.put("type", type);
        obj.put("name", service.getServiceName());
        obj.put("port", service.getPort());
        
        // Debug logging
        Log.d("ZeroConfPlugin", "Service: " + service.getServiceName() + 
                ", Port: " + service.getPort() + 
                ", Host: " + (service.getHost() != null ? service.getHost().toString() : "null"));
        
        // Get hostname from host address
        InetAddress host = service.getHost();
        if (host != null) {
            obj.put("hostname", host.getHostName());
            
            // For NsdServiceInfo, we only get one address at a time
            JSArray ipv4Addresses = new JSArray();
            JSArray ipv6Addresses = new JSArray();
            
            String hostAddress = host.getHostAddress();
            if (hostAddress != null) {
                if (hostAddress.contains(":")) {
                    // IPv6 address
                    ipv6Addresses.put(hostAddress);
                } else {
                    // IPv4 address
                    ipv4Addresses.put(hostAddress);
                }
            }
            
            obj.put("ipv4Addresses", ipv4Addresses);
            obj.put("ipv6Addresses", ipv6Addresses);
        } else {
            obj.put("hostname", "");
            obj.put("ipv4Addresses", new JSArray());
            obj.put("ipv6Addresses", new JSArray());
        }

        // Get TXT record attributes
        JSObject props = new JSObject();
        Map<String, byte[]> attributes = service.getAttributes();
        if (attributes != null) {
            for (Map.Entry<String, byte[]> entry : attributes.entrySet()) {
                String key = entry.getKey();
                byte[] value = entry.getValue();
                if (value != null) {
                    props.put(key, new String(value));
                } else {
                    props.put(key, "");
                }
            }
        }
        obj.put("txtRecord", props);

        return obj;
    }
}
