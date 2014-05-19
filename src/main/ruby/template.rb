
require_relative "engine"
require_relative "state_observer"
require_relative "utils"

require_relative "constructs/argument"
require_relative "constructs/instruction_block"
require_relative "constructs/instruction"
require_relative "constructs/label"
require_relative "constructs/no_value"
require_relative "constructs/output"
require_relative "constructs/situation"

# Header for generated assembly files
HEADER_TEXT =
"; This test program was automatically generated by MicroTESK\n" +
"; Generation started: %s\n;\n" +
"; Institute for System Programming of the Russian Academy of Sciences (ISPRAS)\n" +
"; 25, Alexander Solzhenitsyn st., Moscow, 109004, Russia\n" +
"; http://forge.ispras.ru/projects/microtesk\n\n"

class Template
  include StateObserver

  @@template_classes = Array.new
  attr_accessor :is_executable

  def initialize
    super

    # Settings (can be overridden by the user)
    @is_executable = true
    @use_stdout    = true
    @log_execution = true

    @sl_comment_starts_with = "//" # single-line comment characters 
    @ml_comment_starts_with = "/*" # multi-line comment start start characters
    @ml_comment_ends_with   = "*/" # multi-line comment end characters 

    # Important variables for core Template functionality
    @core_block = InstructionBlock.new

    @instruction_receiver = @core_block
    @receiver_stack = [@core_block]

    @final_sequences = Array.new
  end

  def self.template_classes
    @@template_classes
  end

  def self.set_model(j_model)
    Engine.model = j_model
    StateObserver.model = j_model
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

  # Run... pre, run and post! This method parses the template
  def parse
    pre
    run
    post
  end

  def block(attributes = {}, &contents)
    b = InstructionBlock.new
    b.attributes = attributes

    @receiver_stack.push b
    @instruction_receiver = @receiver_stack.last

    self.instance_eval &contents

    @receiver_stack.pop
    @instruction_receiver = @receiver_stack.last

    @instruction_receiver.receive b
  end

  def label(name)
    l = Label.new
    l.name = name.to_s
    @instruction_receiver.receive l
  end

  # Prints text into the simulator execution log
  def trace(string)
    @instruction_receiver.receive OutputString.new(string, true)
  end

  def trace_(&block)
    @instruction_receiver.receive OutputCode.new(block, true)
  end

  # --- Methods for printing text to output

  # Puts the new line character into the test program
  def newline
    text '' 
  end

  #  Puts text into the test program
  def text(string)
    @instruction_receiver.receive OutputString.new(string)
  end

  # Evaluates a code block at printing time and puts the resulting text into a test program  
  def text_(&block)
    @instruction_receiver.receive OutputCode.new(block)
  end

  # Puts a comment into the test program (uses @sl_comment_starts_with)
  def comment(string)
    if !string.empty? and string[0] == ?\" then #"
      text string.insert(1, @sl_comment_starts_with)
    else
      text @sl_comment_starts_with + string
    end
  end

  # Starts a multi-line comment (uses @sl_comment_starts_with)
  def start_comment
    text @ml_comment_starts_with
  end

  # Ends a multi-line comment (uses the ML_COMMENT_ENDS_WITH property)
  def end_comment
    text @ml_comment_ends_with 
  end

  # --- Special "no value" method ---

  def _(aug_value = nil)
    NoValue.new(aug_value)
  end

  def __(aug_value = nil)
    v = NoValue.new(aug_value)
    v.is_immediate = true
    v
  end

  # -------------------------------------------------- #
  # Execution                                          #
  # -------------------------------------------------- #

  # TODO: everything

  def execute

    puts
    puts "---------- Start build ----------"
    puts

    bl = @core_block.build(Engine.j_bbf)
    bl_iter = bl.getIterator()

    puts
    puts "---------- Start execute ----------"
    puts

    # Preprocess labels
    @labels = Hash.new

    # look for labels in the sequences
    bl_iter.init()
    sn = 0
    sequences = Array.new
    
    while bl_iter.hasValue()
      seq = bl_iter.value()

      seq.each_with_index do |inst, i|
        #TODO check if sequences have nulls?
        if inst == nil
          next
        end
        
        f_labels = inst.getAttribute("f_labels")
        b_labels = inst.getAttribute("b_labels")

        #process labels
      
        if f_labels.is_a? Array
          f_labels.each do |label|
            @labels[label] = [sn, i + 1]
          end
        end
        
        if b_labels.is_a? Array
          b_labels.each do |label|
            @labels[label] = [sn, i]
          end 
        end        
      end
      sn += 1
      sequences.push seq
      
      bl_iter.next()
    end

    # Execute and generate data in the process
    generated = Array.new
    @final_sequences = Array.new(sequences.length)
    @final_sequences.each_with_index do |sq, i| 
      @final_sequences[i] = nil 
    end
    
    cur_seq = 0
    continue = true
    label = nil
    
    # puts @labels.to_s
    
    # execution loop
    while continue && cur_seq < sequences.length
      fin, label = exec_sequence(sequences[cur_seq], @final_sequences[cur_seq], cur_seq, label)
      
      if @final_sequences[cur_seq] == nil && cur_seq < sequences.length
        @final_sequences[cur_seq] = fin
      end
      
      if label == nil
        goto = cur_seq + 1
      else
        unstack = [] + label[1]
        while @labels[[label.first, unstack]] == nil && unstack.length > 0
          unstack.pop
        end
        
        result = @labels[[label.first, unstack]]
        
        if result == nil
          goto = cur_seq + 1
          text = label.first
          label[1].each do |t|
            text += "_" + t.to_s
          end
          puts "Label " + label.first + " doesn't exist"
        else
          label = [label.first, unstack]
          goto = result.first
        end
      end      
      
      if (goto >= sn + 1) or (goto == -1 && cur_seq >= sn)
        continue = false
      else
        cur_seq = goto
      end
      
    end
      
    # Generate the remaining sequences  
    @final_sequences.each_with_index do |s, i|
      if s == nil && i < sequences.length
#        if sequences[i] == nil
#          puts "what the fuck " + i.to_s
#        end
        @final_sequences[i] = Engine.generate_data sequences[i]
      end
    end

  end
  
  def exec_sequence(seq, gen, id, label)
    r_gen = gen
    if gen == nil
      # TODO NEED EXCEPTION HANDLER
      r_gen = Engine.generate_data seq
    end
    
    labels = Hash.new
    
    r_gen.each_with_index do |inst, i|
      f_labels = inst.getAttribute("f_labels")
      b_labels = inst.getAttribute("b_labels")
      
      #process labels
      
      if f_labels.is_a? Array
        f_labels.each do |f_label|
          labels[f_label] = i + 1
          # puts "Registered f_label " + f_label
        end
      end
      
      if b_labels.is_a? Array
        b_labels.each do |b_label|
          labels[b_label] = i
          # puts "Registered b_label " + b_label
        end        
      end
    end
    
    cur_inst = 0
    
    if label != nil
      cur_inst = labels[label]
      # puts label.to_s
      # puts labels.to_s
    end
    
    total_inst = r_gen.length
    
    continue = true
    
    jump_target = nil
    
    while continue && cur_inst < total_inst
      
      inst = r_gen[cur_inst]
      i_labels = inst.getAttribute("labels")
      
      f_debug = inst.getAttribute("f_runtime")
      b_debug = inst.getAttribute("b_runtime")
      
      exec = inst.getExecutable()

      if b_debug.is_a? Array
        b_debug.each do |b_d|
          b_d_text = b_d.evaluate_to_text(self) 
          if nil != b_d_text
            puts b_d_text
          end
        end
      end

      if @log_execution
        puts exec.getText()
      end

      exec.execute()
      # execute some debug code too

      if f_debug.is_a? Array
        f_debug.each do |f_d|
          f_d_text = f_d.evaluate_to_text(self) 
          if nil != f_d_text
            puts f_d_text
          end
        end
      end

      # Labels
      jump = StateObserver.control_transfer_status

      # TODO: Support instructions with 2+ labels (needs API)
      
      if jump > 0
        target = inst.getAttribute("labels").first.first
        if target == nil || target.first == nil
          puts "Jump to nil label, transfer status: " + jump.to_s
        elsif labels.has_key? target
          cur_inst = labels[target]
          if @log_execution
            text = target.first
            target[1].each do |t|
              text += "_" + t.to_s
            end
            puts "Jump (internal) to label: " + text
          end
          next
        else
          jump_target = target
          if @log_execution
            text = target.first
            target[1].each do |t|
              text += "_" + t.to_s
            end
            puts "Jump (external) to label: " + text

          end
          break
        end
      end
      
      # If there weren't any jumps, continue on to the next instruction
      cur_inst += 1
    end
    
    [r_gen, jump_target]
    
  end

  # Print out the executable program
  def output(filename)

    puts
    puts "---------- Start output ----------"
    puts

    use_file = filename != nil and filename != ""
    if use_file
      file = File.open(filename, 'w')
      file.printf HEADER_TEXT, Time.new
    end

    # prints a string to the output
    text_printer = lambda do |text|
      if use_file
        file.puts text
      end
      if @use_stdout
         puts text
      end
    end

    # prints an array of object evaluating them
    eval_array_printer = lambda do |arr|
      if arr.is_a? Array
        arr.each do |item|
          s = item.evaluate_to_text(self)
          if s != nil
            text_printer.call s
          end
        end
      end
    end
    
    # prints an array of labels to the output 
    label_array_printer = lambda do |arr|
      if arr.is_a? Array
        arr.each do |label|
          text = label.first
          label[1].each do |t|
            text += "_" + t.to_s
          end
          text_printer.call text + ":"
        end
      end
    end

    @final_sequences.each do |fs|
      fs.each do |inst|
        eval_array_printer.call  inst.getAttribute("b_output")
        label_array_printer.call inst.getAttribute("b_labels")

        text_printer.call        inst.getExecutable().getText()

        label_array_printer.call inst.getAttribute("f_labels")
        eval_array_printer.call  inst.getAttribute("f_output")
      end
    end

  end

end
