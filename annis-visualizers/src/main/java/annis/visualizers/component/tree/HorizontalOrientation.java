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
package annis.visualizers.component.tree;

import annis.model.AnnisConstants;
import annis.model.AnnisNode;
import annis.model.RelannisNodeFeature;
import de.hu_berlin.german.korpling.saltnpepper.salt.saltCore.SNode;
import java.util.Comparator;

public enum HorizontalOrientation {
	LEFT_TO_RIGHT(1),
	RIGHT_TO_LEFT(-1);
	
	private final int directionModifier;
	
	HorizontalOrientation(int directionModifier_) {
		directionModifier = directionModifier_;
	}
	
	Comparator<SNode> getComparator() {
		return new Comparator<SNode>() {
			@Override
			public int compare(SNode o1, SNode o2) {
				
        RelannisNodeFeature featNode1 = (RelannisNodeFeature)
          o1.getSFeature(AnnisConstants.ANNIS_NS, AnnisConstants.FEAT_RELANNIS_NODE);
        RelannisNodeFeature featNode2 = (RelannisNodeFeature)
          o2.getSFeature(AnnisConstants.ANNIS_NS, AnnisConstants.FEAT_RELANNIS_NODE);
        
        return directionModifier * (int) (featNode1.getLeftToken() - featNode2.getLeftToken());

			}
		};	
	}
}
