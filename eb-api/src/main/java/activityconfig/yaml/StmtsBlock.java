/*
 *
 *    Copyright 2016 jshook
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * /
 */

package activityconfig.yaml;

import java.util.ArrayList;
import java.util.List;

/**
 * A StmtsDef contains a list of statements, as well as all of the optional
 * block parameters that can be assigned to {@link BlockParams}, which includes
 * a name, config values, data bindings, and filtering tags.
 */
public class StmtsBlock extends BlockParams {

    private List<String> statements = new ArrayList<>();

    public StmtsBlock() {}

    public StmtsBlock(List<String> statements) {
        this.statements = statements;
    }

    public List<String> getStatements() {
        return statements;
    }

    public void setStatements(List<String> statements) {
        this.statements.clear();
        this.statements.addAll(statements);
    }
}
