import java.util.ArrayList;
import java.util.List;

public class pciCollector {

    public static List<pciDevice> collectDevices() {

        List<pciDevice> devices = new ArrayList<>();

        int count = pciInfo.getCount();

        for (int i = 0; i < count; i++) {

            pciDevice device = new pciDevice(
                    pciInfo.getAddress(i),
                    pciInfo.getBus(i),
                    pciInfo.getDevice(i),
                    pciInfo.getFunction(i),
                    pciInfo.getVendorId(i),
                    pciInfo.getDeviceId(i),
                    pciInfo.getClassCode(i));

            devices.add(device);
        }

        return devices;
    }

}
