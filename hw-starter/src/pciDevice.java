public class pciDevice {
    private final String address;
    private final int bus;
    private final int device;
    private final int function;
    private final int vendorId;
    private final int deviceId;
    private final int classCode;

    public pciDevice(String address, int bus, int device, int function, int vendorId, int deviceId, int classCode) {
        this.address = address;
        this.bus = bus;
        this.device = device;
        this.function = function;
        this.vendorId = vendorId;
        this.deviceId = deviceId;
        this.classCode = classCode;
    }

    public String getAddress() {
        return address;
    }

    public int getBus() {
        return bus;
    }

    public int getDevice() {
        return device;
    }

    public int getFunction() {
        return function;
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getClassCode() {
        return classCode;
    }

}
