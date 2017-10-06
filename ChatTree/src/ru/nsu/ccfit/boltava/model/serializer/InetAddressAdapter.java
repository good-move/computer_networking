package ru.nsu.ccfit.boltava.model.serializer;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.net.InetAddress;

public class InetAddressAdapter extends XmlAdapter<String, InetAddress> {
    @Override
    public InetAddress unmarshal(String s) throws Exception {
        return InetAddress.getByName(s);
    }

    @Override
    public String marshal(InetAddress address) throws Exception {
        return address.getHostAddress();
    }
}
