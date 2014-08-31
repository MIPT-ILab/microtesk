#
# Copyright (c) 2014 ISPRAS (www.ispras.ru)
#
# Institute for System Programming of Russian Academy of Sciences
#
# 25 Alexander Solzhenitsyn st. Moscow 109004 Russia
#
# All rights reserved.
#
# template.rb, Aug 15, 2014 5:16:42 PM Andrei Tatarnikov
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Mixins for the Template class
require_relative 'output'

# Other dependencies
require_relative 'template_builder'
require_relative 'utils'

include TemplateBuilder

#
# Description: 
#
# The Settings module describes settings used in test templates and
# provides default values for these settings. It is includes in the
# Template class as a mixin. The settings can be overridden for
# specific test templates. To do this, instance variables must be
# assigned new values in the initialize method of the corresponding
# test template class. 
#
module Settings

  # Specifies whether the template is a concrete template used as
  # a basis for test generation or it is an abstract template designed
  # to be reused by other test templates that inherit from it. In the
  # latter case, no tests are generated.
  # TODO: This feature needs to be reviewed.
  attr_reader :is_executable

  # Print the generated code to the console.
  attr_reader :use_stdout

  # Print instructions being simulated to the console.   
  attr_reader :log_execution

  # Text that starts single-line comments.
  attr_reader :sl_comment_starts_with

  # Text that starts multi-line comments.
  attr_reader :ml_comment_starts_with

  # Text that terminates multi-line comments.
  attr_reader :ml_comment_ends_with

  #
  # Assigns default values to the attributes.
  # 
  def initialize
    @is_executable = true
    @use_stdout    = true
    @log_execution = true

    @sl_comment_starts_with = "// "
    @ml_comment_starts_with = "/*"
    @ml_comment_ends_with   = "*/"
  end

end

class Template
  include Settings
  include Output

  @@model = nil
  @@template_classes = Array.new

  def initialize
    super
  end

  def self.template_classes
    @@template_classes
  end

  def self.set_model(model)

    if nil != @@model
      puts "Model is already assigned."
      return
    end

    TemplateBuilder.define_runtime_methods model.getMetaData
    @@model = model
  end

  # This method adds every subclass of Template to the list of templates to parse
  def self.inherited(subclass)
    @@template_classes.push subclass
  end

  # Hack to allow limited use of capslocked characters
  def method_missing(meth, *args, &block)
    if self.respond_to?(meth.to_s.downcase)
      self.send meth.to_s.downcase.to_sym, *args, &block
    else
      super
    end
  end

  # -------------------------------------------------- #
  # Main template writing methods                      #
  # -------------------------------------------------- #

  # Pre-condition instructions template
  def pre

  end

  # Main instructions template
  def run
    puts "MTRuby: warning: Trying to execute the original Template#run."
  end

  # Post-condition instructions template
  def post

  end

  # -------------------------------------------------- #
  # Methods for template description facilities        #
  # -------------------------------------------------- #

  def block(attributes = {}, &contents)
    blockBuilder = @template.beginBlock

    if attributes.has_key? :compositor
      blockBuilder.setCompositor(attributes[:compositor])
    end

    if attributes.has_key? :combinator
      blockBuilder.setCombinator(attributes[:combinator])
    end

    attributes.each_pair do |key, value|
      blockBuilder.setAttribute(key.to_s, value)
    end

    self.instance_eval &contents

    @template.endBlock
  end

  def label(name)
    @template.addLabel name 
  end

  def rand(from, to)
    if !from.is_a?(Integer) or !to.is_a?(Integer)
      raise MTRubyError, "from #{from} and to #{to} must be integers." 
    end
    engine.newRandom from, to
  end

  def add_output(o)
    @template.addOutput o
  end

  # --- Special "no value" method ---
  # TODO: Not implemented. Left as a requirement. 
  # Should be implemented in the future.
  #
  # def _(aug_value = nil)
  #   NoValue.new(aug_value)
  # end
  #
  # def __(aug_value = nil)
  #   v = NoValue.new(aug_value)
  #   v.is_immediate = true
  #   v
  # end

  # -------------------------------------------------- #
  # Generation (Execution and Printing)                #
  # -------------------------------------------------- #

  def generate(filename)
    java_import Java::Ru.ispras.microtesk.test.TestEngine
    engine = TestEngine.getInstance(@@model)

    engine.setFileName      filename
    engine.setLogExecution  log_execution
    engine.setPrintToScreen use_stdout
    engine.setCommentToken  sl_comment_starts_with

    @template = engine.newTemplate
    pre
    run
    post
    @template.build

    engine.process @template
  end

end
