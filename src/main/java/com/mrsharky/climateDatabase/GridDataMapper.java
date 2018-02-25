/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrsharky.climateDatabase;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

/**
 *
 * @author Julien Pierret
 */
public class GridDataMapper implements ResultSetMapper<GridData>{
    @Override
    public GridData map(int idx, ResultSet rs, StatementContext ctx) throws SQLException {
          return new GridData(
                  rs.getInt("GridBox_ID")
                  , rs.getDouble("Lat")
                  , rs.getDouble("Lon")
                  , rs.getDouble("Value")
          );
    }
}
