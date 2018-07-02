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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse"
})
@XmlRootElement(name = "array")
public class Array {

    @XmlElements({
        @XmlElement(name = "array", type = Array.class),
        @XmlElement(name = "data", type = Data.class),
        @XmlElement(name = "date", type = Date.class),
        @XmlElement(name = "dict", type = Dict.class),
        @XmlElement(name = "real", type = Real.class),
        @XmlElement(name = "integer", type = Integer.class),
        @XmlElement(name = "string", type = String.class),
        @XmlElement(name = "true", type = True.class),
        @XmlElement(name = "false", type = False.class)
    })
    protected List<Object> arrayOrDataOrDateOrDictOrRealOrIntegerOrStringOrTrueOrFalse;

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
     * {@link String }
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
