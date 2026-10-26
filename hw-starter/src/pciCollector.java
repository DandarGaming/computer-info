import java.util.ArrayList;
import java.util.List;

//collects the pci devices using pciInfo and returns a list of pciDevices.  
public class pciCollector {

    public static List<pciDevice> collectDevices() {

        List<pciDevice> devices = new ArrayList<>(); // creates a list of pci devices

        int count = pciInfo.getCount(); // gets the count of pci devices

        for (int i = 0; i < count; i++) { // iterates through all the pci devices using the count from pciInfo

            pciDevice device = new pciDevice(
                    pciInfo.getAddress(i),
                    pciInfo.getBus(i),
                    pciInfo.getDevice(i),
                    pciInfo.getFunction(i),
                    pciInfo.getVendorId(i),
                    pciInfo.getDeviceId(i),
                    pciInfo.getClassCode(i));

            devices.add(device); // adds the device to the list
        }

        return devices; // returns the list of devices
    }

}
