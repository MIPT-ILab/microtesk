#
# Copyright 2018 ISP RAS (http://www.ispras.ru)
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

import template_builder
import globals



class Template:
    template_classes = {}
    
    def __init__(self):
        self.situation_manager = SituationManager(self)
        self.data_manager = None
         
    #def template(self):
     #   return self.template
    
    def get_caller_location(self,caller_index = 1):
        #import inspect
        #(frame, filename, line_number, 
         #function_name, lines, index) = inspect.getouterframes(inspect.currentframe())[2]
        #return self.template.where(filename,line_number)
        return self.template.where("xxx.py",1)
    
    def define_method(self,method_name,method_body):
        method_name = method_name.lower()
        if not hasattr(Template, method_name):
            setattr(Template,method_name,MethodType(method_body,None,Template))
        else:
            print "Error: Failed to define the {} method.".format(method_name)
            
  # ------------------------------------------------------------------------- #
  # Main template writing methods                                             #
  # ------------------------------------------------------------------------- #

  # Pre-condition instructions template     
    def pre(self):
        pass
    
  # Main instructions template    
    def run(self):
        print "Trying to execute the original Template"
        
  # Post-condition instructions template
    def post(self):
        pass
    
    def executed(self,contents = lambda : []):
        return self.set_attributes({'executed' : True}, contents)
        
    def nonexecuted(self,contents = lambda : []):
        return self.set_attributes({'executed' : False}, contents)
    
    def branches(self,contents = lambda : []):
        return self.set_attributes({'branches' : True}, contents)
    
    def set_attributes(self,attributes,contents):
        mapBuilder = set_builder_attributes(self.template.newMapBuilder, attributes)
        self.template.beginAttributes(mapBuilder)
        contents()
        self.template.endAttributes()
        
    def label(self,name):
        if isintance(name, int):
            if not name in range(0,10):
                raise NameError('name should be between 0 and 9')
            
            return self.template.addNumericLabel(name)
            
        else:
            return self.template.addLabel(name,False)
            
    def global_label(self,name):
        return self.template.addLabel(name,True)
        
    def weak(self,name):
        return self.template.addWeakLabel(name)
        
    def label_b(self,index):
        return self.numeric_label_ref(index, False)
        
    def label_f(self,index):
        return self.numeric_label_ref(index, True)
        
    def get_address_of(self,label):
        return self.template.getAddressForLabel(label)
    
    def testdata(name, attrs = {}):
        get_new_situation(name, attrs, True)
        
    def situation(name, attrs = {}):
        get_new_situation(name, attrs, False)
        
    def get_new_situation(name, attrs, testdata_provider):
        if not isinstance(attrs,dict):
            raise TypeError('attrs must be dict')
        
        builder = self.template.newSituation(name, testdata_provider)
        
        for name,value in attrs.iteritems():
            if isintance(value,Dist):
                attr_value = value.java_object
            else:
                attr_value = value
           
            builder.setAttribute(name,attr_value)
            
        return builder.build()
    
    def random_situation(dist):
        return dist.java_object
    
    def set_default_situation(names, situations = lambda : []):
        if not isintance(names,basestring) and not isintance(name,list):
            raise TypeError("names must be String or List.")


        default_situation = self.situation_manager.situations()
        if isinstance(names,list):
            for name in names:
                self.template.setDefaultSituation(name, default_situation)
        else:
            self.template.setDefaultSituation(names, default_situation)
            
    def rand(self,*args):
        if args.count() is 1:
            distribution = args[0]
            
            if not isinstance(distribution,Dist):
                raise TypeError('argument must be a distribution')
            
            return self.template.newRandom(distribution.java_object)
        elif args.count is 2:
            From = args[0]
            To = args[1]
            
            if not instance(From,int) or not instance(To,int):
                raise TypeError('arguments must be integers')
            
            return self.template.newRandom(From,To)
        else:
            raise TypeError('wrong argument count')
            
            
    def dist(self,*ranges):
        if not isintance(ranges,list):
            raise TypeError('ranges is not list')
        
        builder = self.template.newVariateBuilder()
        for range_item in ranges:
            if not isinstance(range_item, ValueRange):
                raise TypeError('range_item is not ValueRange')
            
            value = range_items.value
            bias = range_items.bias
            
            if isintance(value,list):
                if bias is None:
                    builder.addCollection(value)
                else:
                    builder.addCollection(value,bias)
            elif isinstance(value,Dist):
                if bias is None:
                    builder.addVariate(value.java_object)
                else:
                    builder.addVariate(value.java_object,bias)
            else:
                if bias is None:
                    builder.addValue(value)
                else:
                    builder.addValuer(value,bias)
            
        
        return Dist(builder.build())
    
    def range(self,attrs = {}):
        if not isintance(attrs,dict):
            raise TypeError("attrs is not dict")
        
        value = attrs.get('value')
        
        bias = None
        
        bias = attrs.get('bias')
        
        return ValueRange(value,bias)
                
            
                
        
  # -------------------------------------------------------------------------- #
  # Data Definition Facilities                                                 #
  # -------------------------------------------------------------------------- #
    
    def data_config(self,attrs,contents = lambda : []):
        if None != self.data_manager:
            raise NameError('Data configuration is already defined')
        
        target = attrs.get('target')
        
        # Default value is 8 bits if other value is not explicitly specified
        
        if 'item_zise' in attrs:
            addressableSize = attrs.get('item_size')
        else:
            addressableSize = 8
        self.data_manager = DataManager(self, self.template.getDataManager())
        self.data_manager.beginConfig(target,addressableSize)
        
        contents()
        self.data_manager.endConfig()
        
        
        

# -------------------------------------------------------------------------- #
# Sections                                                                   #
# -------------------------------------------------------------------------- #
    def section(self,attrs,contents = lambda : []):
        from java.math import BigInteger        

        name = attrs.get('name')
        
        pa = attrs.get('pa')
        va = attrs.get('va')
        args = attrs.get('args')
        
        pa = BigInteger(str(pa))
        va = BigInteger(str(va))
        
        self.template.beginSection(name,pa,va,args)
        contents()
        self.template.endSection()
        
    
    def section_text(self,attrs,contents = lambda : []):
        from java.math import BigInteger
        
        pa = attrs.get('pa')
        va = attrs.get('va')
        args = attrs.get('args')
        
        pa = BigInteger(str(pa))
        va = BigInteger(str(va))
        
        
        
        
        self.template.beginSectionText(pa,va,args)
        contents()
        self.template.endSection()
        
    def section_data(self,attrs,contents = lambda : []):
        from java.math import BigInteger
        
        pa = attrs.get('pa')
        va = attrs.get('va')
        args = attrs.get('args')
        
        pa = BigInteger(str(pa))
        va = BigInteger(str(va))
        
        
        
        self.template.beginSectionData(pa,va,args)
        contents()
        self.template.endSection()
    
    def generate(self):
        from ru.ispras.microtesk.test import TestEngine
        engine = TestEngine.getInstance()
        
        
        self.template = engine.newTemplate()
        #define_runtime_methods(engine.getModel().getMetaData())
        
        self.template.beginPreSection()
        self.pre()
        self.template.endPreSection()
        
        
        self.template.beginPostSection()
        self.post()
        self.template.endPostSection()
        
        
        self.template.beginMainSection()
        self.run()
        self.template.endMainSection()
        
        
    def set_option_value(self,name,value):
        from ru.ispras.microtesk.test import TestEngine
        engine = TestEngine.getInstance()
        engine.setOptionValue(name, value)  
        
    def get_option_value(self,name):
        from ru.ispras.microtesk.test import TestEngine
        engine = TestEngine.getInstance()
        engine.getOptionValue(name)
        
    def rev_id(self):
        from ru.ispras.microtesk.test import TestEngine
        engine = TestEngine.getInstance()
        engine.getModel.getRevisionId()
        
    def is_rev(self,id):
        from ru.ispras.microtesk.test import TestEngine
        engine = TestEngine.getInstance()
        engine.isRevision(id)
        
    def set_builder_attributes(self,builder,attributes):
        for key in attributes:
            value = attributes[key]
            if isinstance(value, dict):
                mapBuilder = set_builder_attributes(self.template.newMapBuilder(), value)
                builder.setAttribute(key, mapBuilder.getMap())
            else:
                builder.setAttribute(key,value)
                
        return builder
    
    def numeric_label_ref(self,index,forward):
        if not isinstance(index,it):
            raise TypeError('index is not integer')
        if not index in range(0,10):
            raise TypeError('index should be within the range 0..9')
        
        return self.template.newNumericLabelRef(index,forward)
            

class SituationManager:
    def __init__(self,template):
        self.template = template
        
class DataManager:
    
    class Type:
        def __init__(self,*args):
            self.name = args[0]
            self.args = [args[i] for i in range(1,len(args))] if len(args) > 1 else []
            
        def name(self):
            return self.name
        
        def args(self):
            return self.args
        
    def __init__(self,template,manager):
        self.template = template
        self.manager = manager
        
        self.builder = None
        self.ref_count = 0
        
    def beginConfig(self,target,addressableSize):
        self.configurer = self.manager.beginConfig(target,addressableSize)
    
    def endConfig(self):
        self.manager.endConfig()
        self.configurer = None
        
    def beginData(self,global1,separate_file):
        if self.ref_count == 0:
            self.builder = self.template.template.beginData(global1,separate_file)
        
        self.ref_count = self.ref_count + 1
        return self.builder
    
    def endData(self):
        self.ref_count  = self.ref_count - 1
        if self.ref_count == 0:
            self.template.template.endData()
            self.builder = None
    
    def align(self,value):
        value_in_bytes = self.template.alignment_in_bytes(value)
        return self.builder.align(value,value_in_bytes)
    
    def org(self,origin):
        if type(origin) is int:
            self.builder.setOrigin(origin)
        elif type(origin) is dict:
            delta = origin.get('delta')
            if type(delta) is not int:
                raise TypeError("delta must be int")
            self.builder.setRelativeOrigin(delta)
        else:
            raise TypeError("origin must be int or dict")
            
    def type(self,*args):
        return DataManager.Type(*args)
    
    def label(self,id):
        self.builder.addLabel(id,False)
    
    def global_label(self,id):
        self.builder.addLabel(id,True)
    
    def rand(self,from1,to):
        return self.template.rand(from1,to)
        
    def dist(self,*ranges):
        return self.template.dist(*ranges)
    
    def range(self,attrs):
        return self.template.range(attrs)
    
    def define_type(self,attrs):
        id = attrs.get('id')
        text = attrs.get('text')
        type = attrs.get('type')
        
        self.configurer.defineType(id,text,type.name,type.args)
        
        def p(*arguments):
            dataBuilder = self.builder.addDataValues(id)
            for x in arguments:
                dataBuilder.add(x)
            dataBuilder.build()
        
        template_builder.define_method_for(DataManager,id,'type',p)
        
    def define_space(self,attrs):
        from java.math import BigInteger
        id = attrs.get('id')
        text = attrs.get('text')
        fillWith = attrs.get('fill_with')
        
        self.configurer.defineSpace(id,text,BigInteger(str(fillWith)))
        
        def p(length):
            self.builder.addSpace(length)
        
        template_builder.define_method_for(DataManager,id,'space',p)
        
    def define_ascii_string(self,attrs):
        id = attrs.get('id')
        text = attrs.get('text')
        zeroTerm = attrs.get('zero_term')
        
        self.configurer.defineAsciiString(id,text,zeroTerm)
        
        def p(*strings):
            self.builder.addAsciiStrings(zeroTerm,strings)
        
        template_builder.define_method_for(DataManager,id,'string',p)
        
    def text(self,value):
        return self.builder.addText(value)
        
    def comment(self,value):
        return self.builder.addComment(value)
        
    def value(self,*args):
        return self.template.value(*args)
        
    def data(self,contents = lambda : []):
        contents()
        
        
        
        
        
        
        
        
        
        
        
        
            
class WrappedObject:
    def java_object(self):
        raise NotImplementedError('Method java_object is not implemented')
        
class AddressReference(WrappedObject):
    def __init__(self):
        WrappedObject.__init__(self)
        self.template = template
        self.level  = 0
    
    def box(self,arg):
        self.level = arg
        return self
    
    def java_object(self):
        return template.newAddressReference(self.level)
    
    def call(self,min,max):
        return bits(min,max)
    
    def bits(self,min,max):
        return self.template.newAddressReference(self.level,min,max)
        
class BufferEntryReference(WrappedObject):
    def _init__(self):
        WrappedObject.__init__(self)
        self.template = template
        self.level = 0
        
    def box(self,arg):
        self.level = arg
        return self
    
    def java_object(self):
        return self.template.newEntryReference(self.level)
    
    def call(self,min,max):
        return bits(min,max)
    
    def bits(self,min,max):
        return self.template.newEntryReference(self.level,min,max)
        
        
class PageTable:
    def __init__(self,template,data_manager):
        self.template = template
        self.data_manager = data_manager
        
    def text(self,value):
        return self.data_manager.text(value)
     
    def page_table_preparator(self,contents = lambda : []):
        self.preparator = contents
        return self.preparator
    
    def page_table_adaptor(self,contents = lambda : []):
        self.adapter = contents
        return self.adapter
    
    def org(self,address):
        return self.data_manager.org(address)
    
    def align(self,value):
        return self.data_manager.align(value)
        
    def label(self,id):
        return self.data_manager.label(id)
    
    def global_label(self,id):
        return self.data_manager.global_label(id)
        
    def memory_object(self,attrs):
        return self.template.memory_object(attrs)
        
    def page_table_entry(self,attrs):
        from java.ru.ispras.microtesk.test.template import MemoryObject
        
        if type(attrs) is dict:
            try:
                self.preparator
            except NameError:
                print "page_table_preparator is not defined."
            
            prep = self.preparator
            entry = Entry(attrs)
            exec(prep)
        elif type(attrs) is MemoryObject:
            try:
                self.adapter
            except NameError:
                print "page_table_adapter is not defined"
            exec(self.adapter)
        
    class Entry:
        def __init__(self,attrs):  
            if type(attrs) is not dict:
                raise TypeError("attrs must be dict")
            self.attrs = attrs
            
            
class Dist:
    def __init__(self,java_object):
        self.java_object = java_object
    
    def java_object(self):
        return self.java_object
        
    

    def next_value(self):
        return self.java_object.value()
    
class ValueRange:
    def initialize(self,value, bias):
        self.value = value
        self.bias = bias
    

            
            
def sequence(attributes,contents = lambda : []):
    blockBuilder = globals.template.template.beginBlock()
    blockBuilder.setWhere(globals.template.get_caller_location())

    blockBuilder.setAtomic(False)
    blockBuilder.setSequence(True)
    blockBuilder.setIterate(False)

    if 'obfuscator' in attributes:
      blockBuilder.setObfuscator(attributes['obfuscator'])

    globals.template.set_builder_attributes(blockBuilder, attributes)
    contents()

    return globals.template.template.endBlock()

def atomic(attributes,contents = lambda : []):
    blockBuilder = globals.template.template.beginBlock()
    blockBuilder.setWhere(globals.template.get_caller_location())

    blockBuilder.setAtomic(True)
    blockBuilder.setSequence(False)
    blockBuilder.setIterate(False)

    if 'obfuscator' in attributes:
      blockBuilder.setObfuscator(attributes['obfuscator'])

    globals.template.set_builder_attributes(blockBuilder, attributes)
    contents()

    return globals.template.template.endBlock()
    
def iterate(attributes,contents = lambda : []):
    blockBuilder = globals.template.template.beginBlock()
    blockBuilder.setWhere(globals.template.get_caller_location())

    blockBuilder.setAtomic(False)
    blockBuilder.setSequence(False)
    blockBuilder.setIterate(True)

    if 'obfuscator' in attributes:
      blockBuilder.setObfuscator(attributes['obfuscator'])
      
    if 'rearranger' in attributes:
      blockBuilder.setRearranger(attributes['rearranger'])

    globals.template.set_builder_attributes(blockBuilder, attributes)
    contents()

    return globals.template.template.endBlock()

def block(attributes,contents = lambda : []):
    blockBuilder = globals.template.template.beginBlock()
    blockBuilder.setWhere(globals.template.get_caller_location())

    blockBuilder.setAtomic(False)
    blockBuilder.setSequence(False)
    blockBuilder.setIterate(False)
    
    if 'combinator' in attributes:
        blockBuilder.setCombinator(attributes['combinator'])

    if 'permutator' in attributes:
      blockBuilder.setPermutator(attributes['permutator'])

    if 'compositor' in attributes:
      blockBuilder.setCompositor(attributes['compositor'])

    if 'rearranger' in attributes:
      blockBuilder.setRearranger(attributes['rearranger'])

    if 'obfuscator' in attributes:
      blockBuilder.setObfuscator(attributes['obfuscator'])

    globals.template.set_builder_attributes(blockBuilder, attributes)
    contents()
    
    return globals.template.template.endBlock()
            
        
        
        
        
        
        
        
        
        
        
        
        
        
             
     
     
     
     
     
     
     
     
     
     
     
     
     
        