package org.jboss.tools.esb.core.module;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jem.util.emf.workbench.ProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IModuleArtifact;
import org.eclipse.wst.server.core.ServerUtil;
import org.eclipse.wst.server.core.model.ModuleArtifactAdapterDelegate;
import org.eclipse.wst.server.core.util.WebResource;
import org.jboss.tools.esb.core.ESBProjectConstant;

public class JBossESBModuleArtifactAdapter extends
		ModuleArtifactAdapterDelegate {

	public JBossESBModuleArtifactAdapter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public IModuleArtifact getModuleArtifact(Object obj) {

		IResource resource = null;
		if (obj instanceof IResource)
			resource = (IResource) obj;
		else if (obj instanceof IAdaptable)
			resource = (IResource) ((IAdaptable) obj).getAdapter(IResource.class);
		
		if (resource == null)
			return null;
		
		if (resource instanceof IProject) {
			IProject project = (IProject) resource;
			if (isESBProject(project))
				return new WebResource(getModule(project), new Path("")); //$NON-NLS-1$
			return null;	
		}
		IProject project = ProjectUtilities.getProject(resource);
		if (project != null && !isESBProject(project))
			return null;
		
		IVirtualComponent comp = ComponentCore.createComponent(project);
		// determine path
		IPath rootPath = comp.getRootFolder().getProjectRelativePath();
		IPath resourcePath = resource.getProjectRelativePath();

		// Check to make sure the resource is under the Application directory
		if (resourcePath.matchingFirstSegments(rootPath) != rootPath.segmentCount())
			return null;


		// return Web resource type
		return new WebResource(getModule(project), resourcePath);

	}
	
	protected static IModule getModule(IProject project) {
		if (isESBProject(project))
			return ServerUtil.getModule(project);
		return null;
	}
	
	protected static boolean isESBProject(IProject project) {
		return isProjectOfType(project, ESBProjectConstant.ESB_PROJECT_FACET);
	}
	
	protected static boolean isProjectOfType(IProject project, String typeID) {
		IFacetedProject facetedProject = null;
		try {
			facetedProject = ProjectFacetsManager.create(project);
		} catch (CoreException e) {
			return false;
		}

		if (facetedProject != null && ProjectFacetsManager.isProjectFacetDefined(typeID)) {
			IProjectFacet projectFacet = ProjectFacetsManager.getProjectFacet(typeID);
			return projectFacet != null && facetedProject.hasProjectFacet(projectFacet);
		}
		return false;
	}

}