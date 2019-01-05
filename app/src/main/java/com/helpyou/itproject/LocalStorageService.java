
/*
 * Author:
 *
 * Dennis Goyal     | ID:776980
 *
 */

package com.helpyou.itproject;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.ArrayList;

public interface LocalStorageService<E> {

    public ArrayList<E> getAll() throws IOException, SAXException, ParserConfigurationException;

    public boolean add(E element) throws IOException, SAXException, ParserConfigurationException, TransformerException;

    public boolean deleteAll();

    public boolean replaceAll(ArrayList<E> elements) throws IOException, TransformerException, ParserConfigurationException;

}
