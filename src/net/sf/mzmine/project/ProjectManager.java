/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.project;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * This interface defines project manager that deal with inter-project
 * operations
 */

public interface ProjectManager {
	public void createProject(File projectDir) throws IOException;

	public void createTemporalProject() throws IOException;

	public void openProject(File projectDir) throws IOException;
	public void openProject(File projectDir,HashMap<String,Object> options) throws IOException;

	public void saveProject(File projectDir) throws IOException;
	public void saveProject(File projectDir,HashMap<String,Object> options) throws IOException;

	
	public void removeProjectDir(File projectDir);
	public ProjectStatus getStatus();
	public void initModule();
}
