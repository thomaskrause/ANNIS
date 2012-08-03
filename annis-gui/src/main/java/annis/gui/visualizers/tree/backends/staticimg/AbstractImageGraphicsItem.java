/*
 * Copyright 2009-2011 Collaborative Research Centre SFB 632 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.gui.visualizers.tree.backends.staticimg;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import annis.gui.visualizers.tree.GraphicsItem;

public abstract class AbstractImageGraphicsItem implements GraphicsItem {
	private List<AbstractImageGraphicsItem> children = new ArrayList<AbstractImageGraphicsItem>();
	private int zValue;
	
	public void addChildItem(AbstractImageGraphicsItem childItem) {
		children.add(childItem);
	}
	
	public abstract void draw(Graphics2D canvas);
	
	@Override
	public void setParentItem(GraphicsItem parent) {
		((AbstractImageGraphicsItem)parent).addChildItem(this);
	}
	
	@Override
	public void setZValue(int newZValue) {
		zValue = newZValue;
	}
	
	public int getZValue() {
		return zValue;
	}

	public Collection<AbstractImageGraphicsItem> getChildren() {
		return children;
	}

	public void getAllChildren(List<AbstractImageGraphicsItem> outputList) {
		outputList.addAll(children);
		for (AbstractImageGraphicsItem child: children) {
			child.getAllChildren(outputList);
		}
	}
}