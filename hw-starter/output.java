import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class output {
    public static void main(String[] args) {

        System.out.println("\n============================");
        System.out.println("    COMPUTER INFORMATION    ");
        System.out.println("============================\n");

        System.out.println("============================");
        System.out.println("            CPU             ");
        System.out.println("============================\n");

        processor.getCpuInfo();

        System.out.println("\n============================");
        System.out.println("           MEMORY           ");
        System.out.println("============================\n");

        memory.getMemInfo();

        System.out.println("\n============================");
        System.out.println("            PCI             ");
        System.out.println("============================\n");

        List<pciDevice> devices = pciCollector.collectDevices(); // puts collected pci devices in list

        idLookup lookup; // id lookup - there's one in pciTest but it wasn't like a function so I just
                         // put it here and changed it to print stuff out the same way as the usb devices
        try {
            lookup = idLookup.pci(); // try load pci.ids
        } catch (IOException e) { // print error if pci.ids can't be loaded
            System.out.println("Could not load pci.ids: " + e.getMessage());
            System.out.println("Displaying raw IDs instead.");
            lookup = null;
        }

        for (pciDevice device : devices) { // iterates through each device in list to print out info

            String vendorName = lookup == null // condition
                    ? null // vendorName value if lookup == null
                    : lookup.vendorName(device.getVendorId()); // vendorName value if lookup != null

            String deviceName = lookup == null // condition
                    ? null // deviceName value if lookup == null
                    : lookup.deviceName(
                            device.getVendorId(), // deviceName value if lookup != null
                            device.getDeviceId());

            String className = lookup == null // condition
                    ? null // className value if lookup == null
                    : lookup.className(device.getClassCode()); // className value if lookup != null

            if (vendorName == null) { // if lookup failed
                vendorName = String.format(
                        "Unknown vendor (0x%04x)", // prints id converted to hex
                        device.getVendorId());
            }

            if (deviceName == null) { // if lookup failed
                deviceName = String.format(
                        "Unknown device (0x%04x)", // prints id converted to hex
                        device.getDeviceId());
            }

            if (className == null) { // if lookup failed
                className = String.format(
                        "Unknown class (0x%06x)", // prints id converted to hex
                        device.getClassCode());
            }

            System.out.println("Name: " + device.getAddress()); // prints address name

            System.out.println("Vendor ID: "
                    + String.format("%04x", device.getVendorId())); // prints vendor id

            System.out.println("Device ID: "
                    + String.format("%04x", device.getDeviceId())); // prints device id

            System.out.println("Vendor Name: " + vendorName); // prints vendor name

            System.out.println("Device Name: " + deviceName); // prints device name

            System.out.println("Class: " + className); // prints class name

            System.out.println();
        }

        System.out.println("============================");
        System.out.println("         PCI ANALYIS        ");
        System.out.println("============================\n");

        pciAnalysisTest.getPciAnalyis();

        System.out.println("\n============================");
        System.out.println("            USB             ");
        System.out.println("============================\n");

        ArrayList<ArrayList<String>> usbs = usbDevices.getUsbInfo();

        for (ArrayList<String> usb : usbs) {

            System.out.println("Name: " + usb.get(0));
            System.out.println("Vendor ID: " + usb.get(1));
            System.out.println("Product ID: " + usb.get(2));
            System.out.println("Vendor Name: " + usb.get(3));
            System.out.println("Product Name: " + usb.get(4));

            System.out.println();
        }
    }

}
