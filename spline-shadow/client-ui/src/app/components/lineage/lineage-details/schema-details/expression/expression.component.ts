/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, Input } from '@angular/core';
import { Expression } from 'src/app/model/expression';
import { LineageGraphService } from 'src/app/services/lineage/lineage-graph.service';

@Component({
  selector: 'app-expression',
  templateUrl: './expression.component.html',
  styleUrls: ['./expression.component.less']
})
export class ExpressionComponent {

  @Input()
  expressionType: string

  @Input()
  expressions: Expression[]

  constructor(
    private lineageGraphService: LineageGraphService
  ) { }

  getIcon(): string {
    return "&#x" + this.lineageGraphService.getIconFromOperationType(this.expressionType) + "&nbsp;"
  }

  getOperationColor(): string {
    return this.lineageGraphService.getColorFromOperationType(this.expressionType)
  }
}
