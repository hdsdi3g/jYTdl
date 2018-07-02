//
// Ce fichier a été généré par l'implémentation de référence JavaTM Architecture for XML Binding (JAXB), v2.2.8-b130911.1802 
// Voir <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Toute modification apportée à ce fichier sera perdue lors de la recompilation du schéma source. 
// Généré le : 2018.04.01 à 01:58:36 AM CEST 
//


package com.apple.propertylist;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.NormalizedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse"
})
@XmlRootElement(name = "plist")
public class Plist {

    @XmlAttribute(name = "version")
    @XmlJavaTypeAdapter(NormalizedStringAdapter.class)
    protected java.lang.String version;
    @XmlElements({
        @XmlElement(name = "array", required = true, type = Array.class),
        @XmlElement(name = "data", required = true, type = Data.class),
        @XmlElement(name = "date", required = true, type = Date.class),
        @XmlElement(name = "dict", required = true, type = Dict.class),
        @XmlElement(name = "real", required = true, type = Real.class),
        @XmlElement(name = "integer", required = true, type = Integer.class),
        @XmlElement(name = "string", required = true, type = com.apple.propertylist.String.class),
        @XmlElement(name = "true", required = true, type = True.class),
        @XmlElement(name = "false", required = true, type = False.class)
    })
    protected List<Object> arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse;

    /**
     * Obtient la valeur de la propriété version.
     * 
     * @return
     *     possible object is
     *     {@link java.lang.String }
     *     
     */
    public java.lang.String getVersion() {
        if (version == null) {
            return "1.0";
        } else {
            return version;
        }
    }

    /**
     * Définit la valeur de la propriété version.
     * 
     * @param value
     *     allowed object is
     *     {@link java.lang.String }
     *     
     */
    public void setVersion(java.lang.String value) {
        this.version = value;
    }

    /**
     * Gets the value of the arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Array }
     * {@link Data }
     * {@link Date }
     * {@link Dict }
     * {@link Real }
     * {@link Integer }
     * {@link com.apple.propertylist.String }
     * {@link True }
     * {@link False }
     * 
     * 
     */
    public List<Object> getArrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse() {
        if (arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse == null) {
            arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse = new ArrayList<Object>();
        }
        return this.arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse;
    }

}
