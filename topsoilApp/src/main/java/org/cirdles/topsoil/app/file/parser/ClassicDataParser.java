package org.cirdles.topsoil.app.file.parser;

import org.cirdles.topsoil.app.data.DataTable;
import org.cirdles.topsoil.app.data.column.DataColumn;
import org.cirdles.topsoil.variable.Variable;
import org.cirdles.topsoil.variable.Variables;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ClassicDataParser extends DefaultDataParser {

    //**********************************************//
    //                PUBLIC METHODS                //
    //**********************************************//

    /** {@inheritDoc} */
    @Override
    public DataTable parseDataTable(Path path, String delimiter, String label) throws IOException {
        DataTable table = super.parseDataTable(path, delimiter, label);
        prepareTable(table);
        return table;
    }

    /** {@inheritDoc} */
    @Override
    public DataTable parseDataTable(String content, String delimiter, String label) {
        DataTable table = super.parseDataTable(content, delimiter, label);
        prepareTable(table);
        return table;
    }

    //**********************************************//
    //                PRIVATE METHODS               //
    //**********************************************//

    private void prepareTable(DataTable table) {
        List<Variable<?>> variables = Variables.ALL;
        List<DataColumn<?>> columns = table.getDataColumns();

        for (int i = 0; i < Math.min(variables.size(), table.getColumnRoot().countLeafNodes()); i++) {
            table.setColumnForVariable(variables.get(i), columns.get(i));
        }
    }

}
