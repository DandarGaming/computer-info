public class pciDevice {

    // fields of the pci device
    private final String address; // pci address
    private final int bus; // pci bus
    private final int device; // pci device
    private final int function; // pci function
    private final int vendorId; // pci vendor id
    private final int deviceId;
    private final int classCode;

    public pciDevice(String address, int bus, int device, int function, int vendorId, int deviceId, int classCode) {
        this.address = address; // constructs a pci device from the arguments
        this.bus = bus;
        this.device = device;
        this.function = function;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;
    }

    public String getAddress() {
        return address; // returns the pci address
    }

    public int getBus() {
        return bus; // returns the pci bus
    }

    public int getDevice() {
        return device; // returns the pci device
    }

    public int getFunction() {
        return function; // returns the pci function
    }

    public int getVendorId() {
        return vendorId; // returns the pci vendor id
    }

    public int getDeviceId() {
        return deviceId; // returns the pci device id
    }

    public int getClassCode() {
        return classCode; // returns the pci class code
    }

    @Override
    public String toString() { // this is for the jtable to display
        return "PciDevice: " + address +
                " class=" + classCode +
                " vendor=" + vendorId +
                " device=" + deviceId +
                " bus=" + bus +
                " slot=" + device +
                " function=" + function;
    }

}
