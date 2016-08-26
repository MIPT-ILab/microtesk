#
# Copyright 2016 ISP RAS (http://www.ispras.ru)
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

require_relative 'minimips_base'

#
# Description:
#
# This test template demonstrates how to generate test cases
# by composing randomly selected blocks.
#
class RandomSituationTemplate < MiniMipsBaseTemplate

  def pre
    super

    # Start address
    org 0x00020000
  end

  def run
    data {
      label :variable
      word 0xDEADBEEF
    }

    my_dist = dist(
      range(:value => alu_sequence, :bias => 30),
      range(:value => ls_sequence,  :bias => 35),
      range(:value => bpu_sequence, :bias => 15))

    block(:combinator => 'diagonal',
          :compositor => 'catenation',
          :permutator => 'random') {
      4.times {
        my_dist.next_value.add
      }
      epilogue {
        label :exit
        nop
      }
    }.run
  end

  def alu_sequence
    sequence {
      add reg(_), reg(_), reg(_)
      sub reg(_), reg(_), reg(_)
      AND reg(_), reg(_), reg(_)
      OR  reg(_), reg(_), reg(_)
    }
  end

  def ls_sequence
    sequence {
      la r=get_register, :variable
      lw reg(_), 0, r
      sw reg(_), 0, r
    }
  end

  def bpu_sequence
    sequence {
      beq reg(_), reg(_), :exit
      nop
      bne reg(_), zero, :exit
      nop
    }
  end

end
