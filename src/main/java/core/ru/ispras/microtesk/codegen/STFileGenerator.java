/*
 * Copyright 2012-2015 ISP RAS (http://www.ispras.ru)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package ru.ispras.microtesk.codegen;

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.STGroupFile;

import ru.ispras.fortress.util.InvariantChecks;
import ru.ispras.microtesk.SysUtils;
import ru.ispras.microtesk.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * The STFileGenerator class implements logic that generates a source code file
 * from string templates.
 *
 * @author <a href="mailto:andrewt@ispras.ru">Andrei Tatarnikov</a>
 */
public final class STFileGenerator implements FileGenerator {
  private final String outputFile;
  private final String[] templateGroupFiles;
  private final STBuilder templateBuilder;

  /**
   * Creates a class code generator parameterized with a hierarchy template groups, with a builder
   * that will initialized the class code template and with the full name to the target output file.
   *
   * @param outputFile The full name of the target output file.
   * @param templateGroupFiles List of template group files. Important: the order is from the root
   *        of the hierarchy to child groups.
   * @param templateBuilder Builder that is responsible for initialization of the template.
   */
  public STFileGenerator(
      final String outputFile,
      final String[] templateGroupFiles,
      final STBuilder templateBuilder) {
    InvariantChecks.checkNotNull(outputFile);
    InvariantChecks.checkNotEmpty(templateGroupFiles);
    InvariantChecks.checkNotNull(templateBuilder);

    this.outputFile = outputFile;
    this.templateGroupFiles = templateGroupFiles;
    this.templateBuilder = templateBuilder;
  }

  /**
   * Generates the target file.
   *
   * @throws IOException It is raised if the methods fails to create the target file.
   */
  @Override
  public void generate() throws IOException {
    final STGroup group = loadTemplateGroups();
    final ST template = templateBuilder.build(group);

    saveTemplate(template);
  }

  /**
   * Loads template groups from the file system and organizes then into a hierarchy.
   *
   * @return A hierarchy of template groups.
   */
  private STGroup loadTemplateGroups() {
    STGroup parentGroup = null;
    for (final String groupFile : templateGroupFiles) {
      final URL groupUrl = SysUtils.getResourceUrl(groupFile);
      final STGroup group = new STGroupFile(groupUrl, "UTF-8", '<', '>');

      if (null != parentGroup) {
        group.importTemplates(parentGroup);
      }

      parentGroup = group;
    }

    return parentGroup;
  }

  /**
   * Create a file and saves an initialized template to it.
   *
   * @param template An initialized file template.
   * @throws IOException It is raised if the methods fails to create the target file.
   */
  private void saveTemplate(final ST template) throws IOException {
    final File file = FileUtils.newFile(outputFile);
    template.write(file, ErrorListener.get());
  }
}
