#
# Copyright 2014-2016 ISP RAS (http://www.ispras.ru)
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

require ENV['TEMPLATE']

class MiniMipsBaseTemplate < Template

  def initialize
    super
    # Initialize settings here 

    # Sets the indentation token used in test programs
    @indent_token = "\t"

    # Sets the token used in separator lines printed into test programs
    @separator_token = "="
  end

  def pre
    #
    # Information on data types to be used in data sections.
    #
    data_config(:text => '.data', :target => 'M') {
      define_type :id => :byte, :text => '.byte', :type => type('card', 8)
      define_type :id => :half, :text => '.half', :type => type('card', 16)
      define_type :id => :word, :text => '.word', :type => type('card', 32)

      define_space        :id => :space,  :text => '.space',  :fill_with => 0
      define_ascii_string :id => :ascii,  :text => '.ascii',  :zero_term => false
      define_ascii_string :id => :asciiz, :text => '.asciiz', :zero_term => true
    }

    #
    # Simple exception handler. Continues execution from the next instruction.
    #
    exception_handler {
      section(:org => 0x380, :exception => ['IntegerOverflow', 'SystemCall', 'Breakpoint']) {
        trace 'Exception handler (EPC = 0x%x)', location('COP0_R', 14)
        mfc0 ra, cop0(14)
        addi ra, ra, 4
        jr ra 
        nop
      }
    }

    #
    # The code below specifies an instruction sequence that writes a value
    # to the specified register (target) via the REG addressing mode.
    #
    # Default preparator: It is used when no special case provided below
    # is applicable.
    #
    preparator(:target => 'REG') {
      # There are two ways (variants) to set the target register:
      # (1) by reading a constant value from memory and
      # (2) by using ALU instructions.
      # The variants have probabilities 25 and 75.

      variant(:bias => 25) {
        data {
          # Defines a test-case level constant
          label :preparator_data
          word value
        }

        # Loads the constant to the target register
        la at, :preparator_data
        lw target, 0, at
      }

      variant(:bias => 75) {
        # Inserts a named preparator that initializes the high part.
        prepare target, value(16, 31), :name => 'XXXX0000'

        # Inserts a named preparator that initializes the low part leaving the high part unchanged.
        prepare target, value(0, 15),  :name => '----XXXX'
      }
    }

    #
    # Special case: Target is $zero register. Since it is read only and
    # always equal zero, it makes no sence to initialize it.
    #
    preparator(:target => 'REG', :arguments => {:i => 0}) {
      # Empty
    }

    #
    # Special case: Value equals 0x00000000. In this case, it is
    # more convenient to use $zero register to reset the target.
    #
    preparator(:target => 'REG', :mask => "00000000") {
      # There are three ways (variants) to reset the target register,
      # which are chosen at random with equal probabilities. 

      variant {
        OR target, zero, zero
      }

      variant {
        AND target, zero, zero
      }

      variant {
        xor target, zero, zero
      }
    }

    #
    # Special case: Value equals 0xFFFFFFFF. In this case, it is
    # more convenient to use $zero register to set all bits in the target register.
    #
    preparator(:target => 'REG', :mask => "FFFFFFFF") {
      nor target, zero, zero
    }

    #
    # Special case: Higher half of value is filled with zeros. In this case,
    # it is enough to initialize only the low part.
    #
    preparator(:target => 'REG', :mask => "0000XXXX") {
      # Inserts a named preparator that initializes the low part.
      prepare target, value(0, 15), :name => '0000XXXX'
    }

    #
    # Special case: Lower half of value is filled with zeros. In this case,
    # it is enough to initialize only the high part.
    #
    preparator(:target => 'REG', :mask => "XXXX0000") {
      # Inserts a named preparator that initializes the high part.
      prepare target, value(16, 31), :name => 'XXXX0000'
    }

    #
    # The code below describes named halfword preparators that are used
    # by other preparators to initialize registers.
    #
    # All of them have the same mask, but have different purposes which
    # are identified by their names. Names serve as an additional key to
    # search for a preparator. Such preparators are called only from other
    # preparators and cannot be not used by generation engines directly since
    # engines do not use names. 
    #
    # "XXXX0000": Preparator for initializing the high halfword that resets
    # the low part.
    #
    preparator(:target => 'REG', :name => 'XXXX0000', :mask => 'XXXX') {
      # There are two ways (variants) to set the target register,
      # which are have probabilities 25 and 75. 

      variant(:bias => 25) {
        lui target, value
      }

      variant(:bias => 75) {
        # Inserts a named preparator that initializes the low part.
        prepare target, value, :name => '0000XXXX'
        sll target, target, 16
      }
    }

    #
    # "0000XXXX": Preparator for initializing the low halfword that resets
    # the high part.
    #
    preparator(:target => 'REG', :name => '0000XXXX', :mask => "XXXX") {
      # There are three ways (variants) to initialize the target register,
      # which are chosen at random with equal probabilities.

      variant {
        ori target, zero, value(0, 15)
      }

      variant {
        xori target, zero, value(0, 15)
      }

      variant {
        addi target, zero, value(0, 15)
      }
    }

    #
    # "----XXXX": Preparator for initializing the low halfword that preserves
    # the high part. It assumes that the initial value of the low halfword is 0.
    #
    preparator(:target => 'REG', :name => '----XXXX', :mask => "XXXX") {
      # There are three ways (variants) to initialize the target register,
      # which are chosen at random with equal probabilities.

      variant {
        ori target, target, value(0, 15)
      }

      variant {
        xori target, target, value(0, 15)
      }

      variant {
        addi target, target, value(0, 15)
      }
    }

    # The code below specifies a comparator sequence to be used in self-checking tests
    # to test values in the specified register (target) accessed via the REG
    # addressing mode.
    #
    # Comparators are described using the same syntax as in preparators and can be
    # overridden in the same way..
    #
    # Default comparator: It is used when no special case is applicable.
    #
    comparator(:target => 'REG') {
      prepare target, value

      bne at, target, :check_failed
      nop
    }

    #
    # Special case: Target is $zero register. Since it is read only and
    # always equal zero, it makes no sence to test it.
    #
    comparator(:target => 'REG', :arguments => {:i => 0}) {
      # Empty
    }

    #
    # Special case: Value equals 0x00000000. In this case, it is
    # more convenient to test the target against the $zero register.
    #
    comparator(:target => 'REG', :mask => "00000000") {
      bne zero, target, :check_failed
      nop
    }
  end

  def post
    j :exit
    nop
    newline

    label :check_failed
    comment 'Here must be code for reporting errors detected by self-checks'
    nop
    newline

    label :exit
    comment 'Here must be test program termination code'
    nop
  end

  # Alias for the NOP instruction (MIPS idiom)
  def nop
    sll zero, zero, 0
  end

  # Aliases for accessing General-Purpose Registers
  #   Name    Number Usage                Preserved?
  #   $zero      0   Constant zero
  #   $at        1   Reserved (assembler)
  #   $v0–$v1   2–3  Function result
  #   $a0–$a3   4–7  Function arguments
  #   $t0–$t7  8–15  Temporaries
  #   $s0–$s7  16–23 Saved                    yes
  #   $t8–$t9  24–25 Temporaries
  #   $k0–$k1  26-27 Reserved (OS)
  #   $gp       28   Global pointer           yes
  #   $sp       29   Stack pointer            yes
  #   $fp       30   Frame pointer            yes
  #   $ra       31   Return address           yes

  def zero
    reg(0)
  end

  def at
    reg(1)
  end

  def v0
    reg(2)
  end

  def v1
    reg(3)
  end

  def a0
    reg(4)
  end

  def a1
    reg(5)
  end

  def a2
    reg(6)
  end

  def a3
    reg(7)
  end

  def t0
    reg(8)
  end

  def t1
    reg(9)
  end

  def t2
    reg(10)
  end

  def t3
    reg(11)
  end

  def t4
    reg(12)
  end

  def t5
    reg(13)
  end

  def t6
    reg(14)
  end

  def t7
    reg(15)
  end

  def s0
    reg(16)
  end

  def s1
    reg(17)
  end

  def s2
    reg(18)
  end

  def s3
    reg(19)
  end

  def s4
    reg(20)
  end

  def s5
    reg(21)
  end

  def s6
    reg(22)
  end

  def s7
    reg(23)
  end

  def t8 
    reg(24)
  end

  def t9
    reg(25)
  end

  def k0 
    reg(26)
  end

  def k1 
    reg(27)
  end

  def gp
    reg(28)
  end

  def sp
    reg(29)
  end

  def fp
    reg(30)
  end

  def ra
    reg(31)
  end

  #
  # Shortcut methods to access memory resources in debug messages
  #

  def gpr_observer(index)
    location('GPR', index)
  end

  def mem_observer(index)
    location('M', index)
  end

  #
  # Utility method for printing data stored in memory using labels.
  #
  def trace_data(begin_label, end_label)
    begin_addr = get_address_of(begin_label)
    end_addr = get_address_of(end_label)

    count = (end_addr - begin_addr) / 4
    begin_index = begin_addr / 4 

    trace "\nData starts: 0x%x", begin_addr
    trace "Data ends:   0x%x", end_addr
    trace "Data count:  %d", count

    trace "\nData values:"
    (0..(count-1)).each { |i| 
      word_index = begin_index + i 
      trace "M[0x%x]: %d", word_index, mem_observer(word_index)
    }
    trace ""
  end
end
