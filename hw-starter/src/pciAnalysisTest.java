import java.util.ArrayList;
import java.util.List;
import java.io.IOException;

/**
 * Tests the sorting and analysis methods in pciAnalysis.
 */
public class pciAnalysisTest {

    public static void main(String[] args) {

        // Create a list to hold our test PCI devices.
        List<pciDevice> devices = new ArrayList<>();

        // Add three sample devices with different vendor IDs.
        // These devices are test data; they do not need to exist on this computer.
        devices.add(new pciDevice("0000:00:01.0",
                0, 1, 0, 0x1af4, 0x1000, 0x010000));

        devices.add(new pciDevice("0000:00:02.0",
                0, 2, 0, 0x1414, 0x008a, 0x030000));

        devices.add(new pciDevice("0000:00:03.0",
                0, 3, 0, 0x8086, 0x1234, 0x020000));

        try {
            // Load the PCI vendor and device name lookup data.
            idLookup lookup = idLookup.pci();

            // Sort the devices alphabetically by vendor name.
            List<pciDevice> sortedDevices = pciAnalysis.sortByVendor(devices, lookup);

            // Display the sorted vendor names.
            System.out.println("Devices sorted by vendor name:");

            for (pciDevice device : sortedDevices) {

                // Convert the numeric vendor ID into a readable name.
                String vendorName = lookup.vendorName(device.getVendorId());

                // Display a fallback if the vendor is not in the lookup data.
                if (vendorName == null) {
                    vendorName = String.format(
                            "Unknown vendor (0x%04x)",
                            device.getVendorId());
                }

                System.out.println(vendorName);
            }

            // Sort the sample devices by bus, device, and function.
            List<pciDevice> addressSortedDevices = pciAnalysis.sortByBusDeviceFunction(devices);

            // Print the devices in their sorted order.
            System.out.println("\nDevices sorted by bus, device, and function:");

            for (pciDevice device : addressSortedDevices) {
                // Display the PCI address and its bus/device/function values.
                System.out.println(
                        device.getAddress()
                                + " | Bus: " + device.getBus()
                                + " | Device: " + device.getDevice()
                                + " | Function: " + device.getFunction());
            }

            // Count how many PCI devices belong to each vendor.
            java.util.Map<String, Integer> vendorCounts = pciAnalysis.countByVendor(devices, lookup);

            // Display the summary heading.
            System.out.println("\nPCI Device Summary:");

            // Print each vendor and its device count.
            for (java.util.Map.Entry<String, Integer> entry : vendorCounts.entrySet()) {

                System.out.println(
                        entry.getKey() + ": " + entry.getValue());
            }

            // Display the total number of devices in the test list.
            System.out.println("Total devices: " + devices.size());

        } catch (IOException e) {
            // Explain the problem if the PCI lookup file cannot be loaded.
            System.out.println(
                    "Could not load pci.ids: " + e.getMessage());
        }
    }
}
