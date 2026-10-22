import java.io.IOException;
import java.util.List;

public class pciTest {
    public static void main(String[] args) {
        List<pciDevice> devices = pciCollector.collectDevices();

        idLookup lookup;
        try {
            lookup = idLookup.pci();
        } catch (IOException e) {
            System.out.println("Could not load pci.ids: " + e.getMessage());
            System.out.println("Displaying raw IDs instead.");
            lookup = null;
        }

        System.out.println("Total PCI devices: " + devices.size());

        for (pciDevice device : devices) {
            String vendorName = lookup == null
                    ? null
                    : lookup.vendorName(device.getVendorId());

            String deviceName = lookup == null
                    ? null
                    : lookup.deviceName(
                            device.getVendorId(), device.getDeviceId());

            String className = lookup == null
                    ? null
                    : lookup.className(device.getClassCode());

            if (vendorName == null) {
                vendorName = String.format("Unknown vendor (0x%04x)",
                        device.getVendorId());
            }

            if (deviceName == null) {
                deviceName = String.format("Unknown device (0x%04x)",
                        device.getDeviceId());
            }

            if (className == null) {
                className = String.format("Unknown class (0x%06x)",
                        device.getClassCode());
            }

            System.out.printf(
                    "Address: %s | Bus: %02x | Device: %02x | Function: %x"
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
