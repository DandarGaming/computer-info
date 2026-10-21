import java.util.ArrayList;
//import java.util.List;

public class usbDevices { //main class

    public static ArrayList<ArrayList<String>> getUsbInfo(){ //main function


       int usbCount = usbInfo.getCount();//finds number of usb ports on device

        ArrayList<ArrayList<String>> usbArray = new ArrayList<>();//initiates an empty array of USB's. Only takes Array Lists of type "String" as values to create a nested array.
       
        if(usbCount > 0){ //if device has USB's, code below runs. If device has none or cant acces them (VM, or macbook) else statement runs. 


        for(int i=0; i<usbCount; i++){ //loops for number of usb ports on device

            //initializes new array list that will hold the data from each USB (name,vendorId, productID).
            ArrayList<String> usbData = new ArrayList<>(); 

            int index = i;
            String name = usbInfo.getName(index); //fetches name of the usb.

            int intVendorId = usbInfo.getVendorId(index); //gets vendor ID for USB
            int intProductId = usbInfo.getProductId(index); //gets product ID for USB


            //UsbData only takes strings as arguments. vendor and product need to be changed from Int ----> String
            String vendorId = Integer.toString(intVendorId); 
            String productId = Integer.toString(intProductId);

            
            // adds variables to usbData Array
            usbData.add(name); 
            usbData.add(vendorId);
            usbData.add(productId);

            
            //appends the instance of usbData (an array) to the overarching usbArray. As the loop continues for number of USBs, each USB's data gets added to a new instance of usbData, and that array gets added to the main "UsbArray" holding all device's USBs and their data.
            usbArray.add(usbData);  
       }

       return usbArray;

       }


        else { //runs if there are no USB's on device
        System.out.println("Sorry, no USB devices found. Maybe you're on a VM or Macbook?");
        return usbArray;
       }
       
    }
}
