//
// ���ļ����� JavaTM Architecture for XML Binding (JAXB) ����ʵ�� v2.2.8-b130911.1802 ���ɵ�
// ����� <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// �����±���Դģʽʱ, �Դ��ļ��������޸Ķ�����ʧ��
// ����ʱ��: 2023.10.24 ʱ�� 10:56:57 AM CST 
//


package net.wanji.openx.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>TrafficSignalAction enity
 * 
 * 
 * 
 * <pre>
 * complexType name="TrafficSignalAction">
 *   complexContent>
 *     restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       choice>
 *         element name="TrafficSignalControllerAction" type="{}TrafficSignalControllerAction" minOccurs="0"/>
 *         element name="TrafficSignalStateAction" type="{}TrafficSignalStateAction" minOccurs="0"/>
 *       /choice>
 *     /restriction>
 *   /complexContent>
 * /complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TrafficSignalAction", propOrder = {
    "trafficSignalControllerAction",
    "trafficSignalStateAction"
})
public class TrafficSignalAction {

    @XmlElement(name = "TrafficSignalControllerAction")
    protected TrafficSignalControllerAction trafficSignalControllerAction;
    @XmlElement(name = "TrafficSignalStateAction")
    protected TrafficSignalStateAction trafficSignalStateAction;

    /**
     * ��ȡtrafficSignalControllerAction���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link TrafficSignalControllerAction }
     *     
     */
    public TrafficSignalControllerAction getTrafficSignalControllerAction() {
        return trafficSignalControllerAction;
    }

    /**
     * ����trafficSignalControllerAction���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link TrafficSignalControllerAction }
     *     
     */
    public void setTrafficSignalControllerAction(TrafficSignalControllerAction value) {
        this.trafficSignalControllerAction = value;
    }

    /**
     * ��ȡtrafficSignalStateAction���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link TrafficSignalStateAction }
     *     
     */
    public TrafficSignalStateAction getTrafficSignalStateAction() {
        return trafficSignalStateAction;
    }

    /**
     * ����trafficSignalStateAction���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link TrafficSignalStateAction }
     *     
     */
    public void setTrafficSignalStateAction(TrafficSignalStateAction value) {
        this.trafficSignalStateAction = value;
    }

}
