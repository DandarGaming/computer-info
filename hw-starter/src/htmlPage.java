import java.io.IOException; //in case pci.ids can't be read
import java.util.ArrayList;
import java.util.List;

//builds the whole web page as one big String.
//this class does not save the page anywhere. htmlReport.java saves it to a file,
public class htmlPage {

    public static String buildPage() { //main function, returns the finished page

        //StringBuilder is like a String we can keep adding to.
        //adding to a normal String with + makes a brand new String every time, StringBuilder doesn't.
        StringBuilder html = new StringBuilder();

        //the top of every html page. tells the browser this is html and gives the tab a title.
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset=\"utf-8\">\n"); //so special characters in device names show up properly
        html.append("<title>Computer Info</title>\n");

        //the styling lives in style.css. it needs to be in the same folder as report.html.
        html.append("<link rel=\"stylesheet\" href=\"style.css\">\n");

        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>Computer Info</h1>\n");

        //each section of the page is built by function below
        html.append(cpuSection());
        html.append(memorySection());
        html.append(pciSection());
        html.append(usbSection());

        //close the tags we opened at the top
        html.append("</body>\n");
        html.append("</html>\n");

        //turn the StringBuilder back into a normal String and return it
        return html.toString();
    }


    //builds the CPU part of the page
    public static String cpuSection() {

        StringBuilder html = new StringBuilder();

        String model = cpuInfo.getModel(); //name of the processor
        int coreCount = cpuInfo.getCount(); //number of cores

        html.append("<h2>CPU</h2>\n");
        html.append("<p>Model: " + escape(model) + "</p>\n");
        html.append("<p>Cores: " + coreCount + "</p>\n");

        return html.toString();
    }


    //builds the memory part of the page
    public static String memorySection() {

        StringBuilder html = new StringBuilder();

        //memInfo gives us KiB. dividing by 1024 gives MiB, which is easier to read.
        long totalMiB = memInfo.getTotal() / 1024;
        long usedMiB = memInfo.getUsed() / 1024;
        long availableMiB = memInfo.getAvailable() / 1024;

        html.append("<h2>Memory</h2>\n");
        html.append("<p>Total: " + totalMiB + " MiB</p>\n");
        html.append("<p>Used: " + usedMiB + " MiB</p>\n");
        html.append("<p>Available: " + availableMiB + " MiB</p>\n");

        return html.toString();
    }


    //builds the PCI part of the page as a table with one row per device
    public static String pciSection() {

        StringBuilder html = new StringBuilder();

        html.append("<h2>PCI Devices</h2>\n");

        idLookup lookup; //declare lookup variable

        try {
            lookup = idLookup.pci(); //try load pci.ids so we can show names instead of numbers
        }
        catch (IOException e) { //if pci.ids can't be loaded, we still show the raw ids
            lookup = null;
        }

        //reuse code to get the devices, sorted by bus, device then function
        List<pciDevice> devices = pciCollector.collectDevices();
        List<pciDevice> sortedDevices = pciAnalysis.sortByBusDeviceFunction(devices);

        if (sortedDevices.size() == 0) { //no devices found (e.g. on a mac)
            html.append("<p>No PCI devices found.</p>\n");
            return html.toString();
        }

        //start the table and add the heading row
        html.append("<table>\n");
        html.append("<tr><th>Address</th><th>Vendor</th><th>Device</th><th>Type</th></tr>\n");

        for (int i = 0; i < sortedDevices.size(); i++) { //loops for every pci device

            pciDevice device = sortedDevices.get(i);

            String address = device.getAddress();
            int vendorId = device.getVendorId();
            int deviceId = device.getDeviceId();
            int classCode = device.getClassCode(); //what kind of device it is, e.g. graphics or usb controller

            String vendorName = null;
            String deviceName = null;
            String typeName = null;

            if (lookup != null) { //only look up names if pci.ids loaded
                vendorName = lookup.vendorName(vendorId);
                deviceName = lookup.deviceName(vendorId, deviceId);
                typeName = lookup.className(classCode);
            }

            if (vendorName == null) { //if lookup failed, show the id in hex instead
                vendorName = String.format("Unknown vendor (0x%04x)", vendorId);
            }

            if (deviceName == null) { //if lookup failed, show the id in hex instead
                deviceName = String.format("Unknown device (0x%04x)", deviceId);
            }

            if (typeName == null) { //if lookup failed, show the class code in hex instead
                typeName = String.format("Unknown class (0x%06x)", classCode);
            }

            //one row of the table. <tr> is a row, <td> is a cell in that row.
            html.append("<tr>");
            html.append("<td>" + escape(address) + "</td>");
            html.append("<td>" + escape(vendorName) + "</td>");
            html.append("<td>" + escape(deviceName) + "</td>");
            html.append("<td>" + escape(typeName) + "</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n"); //close the table

        return html.toString();
    }


    //builds the USB part of the page as a table, one row per device.
    //uses getUsbInfo() from usbDevices.java, which already looks up the names for us.
    public static String usbSection() {

        StringBuilder html = new StringBuilder();

        html.append("<h2>USB Devices</h2>\n");

        ArrayList<ArrayList<String>> usbList = usbDevices.getUsbInfo(); //list of usbs, each one is a list of its data

        if (usbList.size() == 0) { //no devices found (e.g. on a VM or mac)
            html.append("<p>No USB devices found.</p>\n");
            return html.toString();
        }

        //start the table and add the heading row
        html.append("<table>\n");
        html.append("<tr><th>Name</th><th>ID</th><th>Vendor</th><th>Product</th></tr>\n");

        for (int i = 0; i < usbList.size(); i++) { //loops for every usb device

            ArrayList<String> usbData = usbList.get(i); //the data for this one usb

            //getUsbInfo() adds the data in this order: name, vendorId, productId, vendorName, productName
            String name = usbData.get(0);
            String vendorId = usbData.get(1);
            String productId = usbData.get(2);
            String vendorName = usbData.get(3);
            String productName = usbData.get(4);

            String id = vendorId + ":" + productId; //shown like lsusb does, e.g 046d:c52b

            //one row of the table
            html.append("<tr>");
            html.append("<td>" + escape(name) + "</td>");
            html.append("<td>" + escape(id) + "</td>");
            html.append("<td>" + escape(vendorName) + "</td>");
            html.append("<td>" + escape(productName) + "</td>");
            html.append("</tr>\n");
        }

        html.append("</table>\n"); //close the table

        return html.toString();
    }


    //some device names contain characters like & or < which html treats as code.
    //this swaps them for "safe" versions so the browser shows them as normal text.
    public static String escape(String text) {

        if (text == null) { //nothing to show i.e trying to escape a variable that doesnt exist (if on mac or VM)
            return "";
        }

        String safeText = text;
        safeText = safeText.replace("&", "&amp;"); //must be done first, otherwise it would break the ones below
        safeText = safeText.replace("<", "&lt;");
        safeText = safeText.replace(">", "&gt;");
        safeText = safeText.replace("\"", "&quot;");

        return safeText;
    }
}
