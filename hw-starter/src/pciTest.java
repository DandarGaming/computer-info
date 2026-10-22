import java.io.IOException; // import the IOException class  
import java.util.List;

public class pciTest {
    public static void main(String[] args) {
        List<pciDevice> devices = pciCollector.collectDevices(); // collecting the pci devices

        idLookup lookup;
        try {
            lookup = idLookup.pci(); // gets the pci.ids file
        } catch (IOException e) {
            System.out.println("Could not load pci.ids: " + e.getMessage());
            System.out.println("Displaying raw IDs instead.");
            lookup = null;
        }

        System.out.println("Total PCI devices: " + devices.size());

        for (pciDevice device : devices) { // looping through the pci devices
            String vendorName = lookup == null // checks if the lookup is null
                    ? null
                    : lookup.vendorName(device.getVendorId()); // gets the vendor name

            String deviceName = lookup == null // checks if the lookup is null
                    ? null
                    : lookup.deviceName(
                            device.getVendorId(), device.getDeviceId()); // gets the device name

            String className = lookup == null // checks if the lookup is null
                    ? null
                    : lookup.className(device.getClassCode()); // gets the class name

            if (vendorName == null) { // checks if the vendor name is null
                vendorName = String.format("Unknown vendor (0x%04x)",
                        device.getVendorId()); // formats the unknown vendor name
            }

            if (deviceName == null) { // checks if the device name is null
                deviceName = String.format("Unknown device (0x%04x)",
                        device.getDeviceId()); // formats the unknown device name
            }

            if (className == null) { // checks if the class name is null
                className = String.format("Unknown class (0x%06x)",
                        device.getClassCode()); // formats the unknown class name
            }

            System.out.printf(
                    "Address: %s | Bus: %02x | Device: %02x | Function: %x" // prints the device information
                            + " | Vendor: %s | Device name: %s | Class: %s%n",
                    device.getAddress(),
                    device.getBus(),
                    device.getDevice(),
                    device.getFunction(),
                    vendorName,
                    deviceName,
                    className);
        }
    }

}
