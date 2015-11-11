//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.11.10 at 04:43:16 PM CET 
//


package net.sll_mdilab.t5;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for Order_Observations complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="Order_Observations">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence minOccurs="0">
 *         &lt;element ref="{http://sll-mdilab.net/T5/}Order" minOccurs="0"/>
 *         &lt;element ref="{http://sll-mdilab.net/T5/}MDS" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element ref="{http://sll-mdilab.net/T5/}Note" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Order_Observations", propOrder = {
    "order",
    "mds",
    "note"
})
public class OrderObservations {

    @XmlElement(name = "Order")
    protected Order order;
    @XmlElement(name = "MDS")
    protected List<MDS> mds;
    @XmlElement(name = "Note")
    protected List<Note> note;

    /**
     * OBR
     * 
     * @return
     *     possible object is
     *     {@link Order }
     *     
     */
    public Order getOrder() {
        return order;
    }

    /**
     * Sets the value of the order property.
     * 
     * @param value
     *     allowed object is
     *     {@link Order }
     *     
     */
    public void setOrder(Order value) {
        this.order = value;
    }

    /**
     * Gets the value of the mds property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the mds property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMDS().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MDS }
     * 
     * 
     */
    public List<MDS> getMDS() {
        if (mds == null) {
            mds = new ArrayList<MDS>();
        }
        return this.mds;
    }

    /**
     * NTE Gets the value of the note property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the note property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getNote().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Note }
     * 
     * 
     */
    public List<Note> getNote() {
        if (note == null) {
            note = new ArrayList<Note>();
        }
        return this.note;
    }

}
