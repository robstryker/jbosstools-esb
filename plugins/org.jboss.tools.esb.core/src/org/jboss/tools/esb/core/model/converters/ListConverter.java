/******************************************************************************* 
 * Copyright (c) 2007 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.tools.esb.core.model.converters;

import java.util.HashMap;
import java.util.Map;

import org.jboss.tools.common.meta.XAttribute;
import org.jboss.tools.common.meta.XChild;
import org.jboss.tools.common.meta.XModelEntity;
import org.jboss.tools.common.model.XModelObject;
import org.jboss.tools.common.model.impl.AnyElementObjectImpl;
import org.jboss.tools.common.model.util.XModelObjectLoaderUtil;
import org.jboss.tools.esb.core.model.ESBConstants;

/**
 * Class for converting out-of-the-box action property that may have list of child tags.
 * 
 * @author Viacheslav Kabanovich
 */
public abstract class ListConverter implements IPropertyConverter {

	public ListConverter() {}

	protected abstract String getPropertyName();

	protected abstract String getItemEntityName();

	public void toSpecific(XModelObject basicAction, XModelObject specificAction) {
		XModelObject p = getBasicProperty(basicAction);
		if(p == null) return;
		XModelObject[] as = p.getChildren();
		for (int i = 0; i < as.length; i++) {
			if(as[i] instanceof AnyElementObjectImpl) {
				XModelObject a = fromAnyElement(as[i], getItemEntityName());
				if(a != null) {
					specificAction.addChild(a);
				}
			}
		}
		p.removeFromParent();
	}

	protected XModelObject getBasicProperty(XModelObject basicAction) {
		return basicAction.getChildByPath(getPropertyName());
	}

	public void toBasic(XModelObject basicAction, XModelObject specificAction) {
		XModelObject[] as = specificAction.getChildren(getItemEntityName());
		if(as.length == 0) return;
		XModelObject p = basicAction.getModel().createModelObject(ESBConstants.ENT_ESB_PROPERTY, null);
		p.setAttributeValue(ESBConstants.ATTR_NAME, getPropertyName());
		for (int i = 0; i < as.length; i++) {
			XModelObject t = toAnyElement(as[i], i);
			p.addChild(t);
		}
		basicAction.addChild(p);
	}

	public XModelObject fromAnyElement(XModelObject any, String toEntity) {
		String tag = any.getAttributeValue("tag");
		Map<String, String> attr = toMap(((AnyElementObjectImpl)any).getAttributes());
		attr.put("#text", "" + any.getAttributeValue("text"));

		XModelObject a = any.getModel().createModelObject(toEntity, null);
		if(!isRelevantTag(tag, a)) {
			return null;
		}
		
		XModelEntity entity = a.getModelEntity();
		XAttribute[] attrs = entity.getAttributes();
		for (int j = 0; j < attrs.length; j++) {
			String name = attrs[j].getName();
			String xml = attrs[j].getXMLName();
			if(xml == null || xml.length() == 0) continue;
			String value = attr.remove(xml);
			if(value == null || value.length() == 0) {
				value = attrs[j].getDefaultValue();
			}
			if(value != null) {
				a.setAttributeValue(name, value);
			}
		}
		if(entity.getAttribute("attributes") != null) {
			attr.remove("#text");
			StringBuffer sb = new StringBuffer();
			for (String n: attr.keySet()) {
				String v = attr.get(n);
				if(sb.length() > 0) sb.append(';');
				sb.append(n).append('=').append(v);
			}
			a.setAttributeValue("attributes", sb.toString());
		}
		
		XModelObject[] cs = any.getChildren();
		if(cs.length > 0 && a.getModelEntity().getChildren().length > 0) {
			//TODO consider case of several child entities, possibly including AnyElement
			if (a.getModelEntity().getChild("AnyElement") != null) {
				for (int j = 0; j < cs.length; j++) {
					a.addChild(cs[j].copy());
				}
			} else {
				for (int j = 0; j < cs.length; j++) {
					String toChildEntity = getToChildEntity(cs[j], a.getModelEntity());
					XModelObject c = fromAnyElement(cs[j], toChildEntity);
					if(c != null) {
						a.addChild(c);
					}
				}				
			}
		}
		return a;
	}

	protected String getToChildEntity(XModelObject any, XModelEntity parent) {
		XChild[] cs = parent.getChildren();
		if(cs.length > 1) {
			String tag = any.getAttributeValue("tag");
			if(tag != null) for (XChild c: cs) {
				XModelEntity e = c.getMetaModel().getEntity(c.getName());
				if(e != null && tag.equals(e.getXMLSubPath())) {
					return e.getName();
				}
			}
		}
		return cs[0].getName();
	}

	protected boolean isRelevantTag(String tag, XModelObject object) {
		return (object != null && tag != null && tag.equals(object.getModelEntity().getXMLSubPath()));
	}

	public XModelObject toAnyElement(XModelObject specific, int index) {
		XModelObject t = specific.getModel().createModelObject("AnyElement", null);
		t.setAttributeValue("tag", getTagName(specific, index));
		StringBuffer sb = new StringBuffer();
		XModelEntity entity = specific.getModelEntity();
		XAttribute[] attrs = entity.getAttributes();
		for (int j = 0; j < attrs.length; j++) {
			String name = attrs[j].getName();
			String xml = attrs[j].getXMLName();
			if(xml == null || xml.length() == 0) continue;
			String value = specific.getAttributeValue(name);
			if(value == null || value.length() == 0 || value.equals(attrs[j].getDefaultValue())) {
				if(!"always".equals(attrs[j].getProperty("save"))) continue;
			}
			if("#text".equals(xml)) {
				t.setAttributeValue("text", value);
			} else {
				if(sb.length() > 0) {
					sb.append(';');
				}
				sb.append(xml).append('=').append(value);
			}
		}
		if(entity.getAttribute("attributes") != null) {
			String as = specific.getAttributeValue("attributes");
			if(as.length() > 0) {
				if(sb.length() > 0) sb.append(';');
				sb.append(as);
			}
		}
		String attributes = sb.toString();
		t.setAttributeValue("attributes", attributes);
		t.setAttributeValue(XModelObjectLoaderUtil.ATTR_ID_NAME, "" + (index + 1));
		XModelObject[] cs = specific.getChildren();
		if (cs.length > 0) {
				for (int j = 0; j < cs.length; j++) {
				XModelObject c = null;
				if (cs[j].getModelEntity().getName().equals("AnyElement")) {
					c = cs[j].copy();
					c.setAttributeValue(XModelObjectLoaderUtil.ATTR_ID_NAME, ""
							+ (j + 1));
				} else {
					c = toAnyElement(cs[j], j);
				}
				if (c != null) {
					t.addChild(c);
				}
			}
		}
		return t;
	}

	protected String getTagName(XModelObject specific, int index) {
		return specific.getModelEntity().getXMLSubPath();
	}

	protected Map<String, String> toMap(String[][] attributes) {
		Map<String,String> map = new HashMap<String, String>();
		for (int i = 0; i < attributes.length; i++) {
			map.put(attributes[i][0], attributes[i][1]);
		}
		return map;
	}

}
