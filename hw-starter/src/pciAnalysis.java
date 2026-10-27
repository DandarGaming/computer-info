import java.util.ArrayList;
import java.util.Comparator; // Tool for comparing and sorting items
import java.util.List;
import java.util.Map; // Used to store key-value pairs, vendor name as key, number of devices as value
import java.util.TreeMap; // Used to store keys in sorted order, so the summary will display vendor names alphabetically

public class pciAnalysis { // Analyses PCI devices by sorting them and producing summaries.

    public static List<pciDevice> sortByVendor( // Sorts the pci devices by vendor name
            List<pciDevice> devices, idLookup lookup) {
        List<pciDevice> sortedDevices = new ArrayList<>(devices); // Copy the original list so we do not change the
                                                                  // collector's list.

        sortedDevices.sort(Comparator.comparing( // sorts the devices alphabetically using their readable vendor names.
                device -> {
                    // Look up the vendor name using its numeric ID.
                    String vendorName = lookup == null
                            ? null
                            : lookup.vendorName(device.getVendorId());

                    // Use a fallback name when the vendor is not recognised.
                    if (vendorName == null) {
                        vendorName = String.format(
                                "Unknown vendor (0x%04x)",
                                device.getVendorId());
                    }

                    // Return the name that the sorting algorithm compares.
                    return vendorName;
                },
                String.CASE_INSENSITIVE_ORDER));

        // Return the sorted copy.
        return sortedDevices;
    }

    public static List<pciDevice> sortByBusDeviceFunction( // Sorts the pci devices by bus, device, and function.
            List<pciDevice> devices) { // Copy the original list so we do not change it.
        List<pciDevice> sortedDevices = new ArrayList<>(devices); // Copy the original list so we do not change it.

        sortedDevices.sort(Comparator.comparingInt( // Sort by bus first, then device, then function.
                pciDevice::getBus)
                .thenComparingInt(pciDevice::getDevice)
                .thenComparingInt(pciDevice::getFunction));

        // Return the sorted copy.
        return sortedDevices;
    }

    public static Map<String, Integer> countByVendor( // Counts the number of PCI devices for each vendor.
            List<pciDevice> devices, idLookup lookup) {

        // Store vendor names and their device counts alphabetically.
        Map<String, Integer> vendorCounts = new TreeMap<>(
                String.CASE_INSENSITIVE_ORDER);

        // Examine each PCI device in the list.
        for (pciDevice device : devices) {

            // Get the readable vendor name from its numeric ID.
            String vendorName = lookup == null
                    ? null
                    : lookup.vendorName(device.getVendorId());

            // Use a fallback if the vendor is not recognised.
            if (vendorName == null) {
                vendorName = String.format(
                        "Unknown vendor (0x%04x)",
                        device.getVendorId());
            }

            // Get the existing count, or start at zero.
            int currentCount = vendorCounts.getOrDefault(vendorName, 0);

            // Increase the count for this vendor by one.
            vendorCounts.put(vendorName, currentCount + 1);
        }

        // Return the completed summary.
        return vendorCounts;
    }

}