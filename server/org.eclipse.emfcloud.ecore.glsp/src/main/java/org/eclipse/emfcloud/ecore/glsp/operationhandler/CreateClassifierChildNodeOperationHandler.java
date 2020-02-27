/********************************************************************************
 * Copyright (c) 2019-2020 EclipseSource and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0, or the MIT License which is
 * available at https://opensource.org/licenses/MIT.
 *
 * SPDX-License-Identifier: EPL-2.0 OR MIT
 ********************************************************************************/
package org.eclipse.emfcloud.ecore.glsp.operationhandler;

import static org.eclipse.glsp.api.jsonrpc.GLSPServerException.getOrThrow;

import java.util.List;
import java.util.function.Function;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EEnumLiteral;
import org.eclipse.emf.ecore.ENamedElement;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emfcloud.ecore.glsp.model.EcoreModelState;
import org.eclipse.emfcloud.ecore.glsp.util.EcoreConfig.Types;
import org.eclipse.glsp.api.action.kind.AbstractOperationAction;
import org.eclipse.glsp.api.action.kind.CreateNodeOperationAction;
import org.eclipse.glsp.api.handler.OperationHandler;
import org.eclipse.glsp.api.model.GraphicalModelState;
import org.eclipse.glsp.graph.GraphPackage;

import com.google.common.base.Preconditions;

public class CreateClassifierChildNodeOperationHandler implements OperationHandler {

	private List<String> handledElementTypeIds = List.of(Types.ATTRIBUTE, Types.OPERATION, Types.ENUMLITERAL);

	@Override
	public boolean handles(AbstractOperationAction operationAction) {
		if (operationAction instanceof CreateNodeOperationAction) {
			CreateNodeOperationAction action = (CreateNodeOperationAction) operationAction;
			return handledElementTypeIds.contains(action.getElementTypeId());
		}
		return false;
	}

	@Override
	public void execute(AbstractOperationAction abstractAction, GraphicalModelState graphicalModelState) {
		Preconditions.checkArgument(abstractAction instanceof CreateNodeOperationAction);
		CreateNodeOperationAction action = (CreateNodeOperationAction) abstractAction;
		EcoreModelState modelState = EcoreModelState.getModelState(graphicalModelState);
		EClassifier container = getOrThrow(modelState.getIndex().getSemantic(action.getContainerId()),
				EClassifier.class, "No valid container with id " + action.getContainerId() + " found");
		String elementTypeId = action.getElementTypeId();
		if (elementTypeId.equals(Types.ATTRIBUTE) && container instanceof EClass) {
			EAttribute attribute = createEAttribute(modelState);
			modelState.getIndex().add(attribute);
			((EClass) container).getEStructuralFeatures().add(attribute);
		} else if (elementTypeId.contentEquals(Types.ENUMLITERAL) && container instanceof EEnum) {
			EEnumLiteral literal = createEEnumLiteral(modelState);
			modelState.getIndex().add(literal);
			((EEnum) container).getELiterals().add(literal);
		}

	}

	protected int setName(ENamedElement namedElement, EcoreModelState modelState) {
		Function<Integer, String> nameProvider = i -> "New" + namedElement.eClass().getName() + i;
		int nodeCounter = modelState.getIndex().getCounter(GraphPackage.Literals.GLABEL, nameProvider);
		namedElement.setName(nameProvider.apply(nodeCounter));
		return nodeCounter;
	}

	protected EEnumLiteral createEEnumLiteral(EcoreModelState modelState) {
		EEnumLiteral literal = EcoreFactory.eINSTANCE.createEEnumLiteral();
		int counter = setName(literal, modelState);
		literal.setValue(counter);
		return literal;

	}

	protected EAttribute createEAttribute(EcoreModelState modelState) {
		EAttribute attribute = EcoreFactory.eINSTANCE.createEAttribute();
		setName(attribute, modelState);
		attribute.setEType(EcorePackage.eINSTANCE.getEString());
		return attribute;
	}

	@Override
	public String getLabel(AbstractOperationAction action) {
		return "Create EClassifier child node";
	}

}
