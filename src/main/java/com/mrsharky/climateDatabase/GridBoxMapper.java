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
public class GridBoxMapper implements ResultSetMapper<GridBox>{
    @Override
    public GridBox map(int idx, ResultSet rs, StatementContext ctx) throws SQLException {
          return new GridBox(
                  rs.getInt("GridBox_ID")
                  , rs.getInt("Lat_ID")
                  , rs.getInt("Lon_ID")
                  , rs.getDouble("Lat")
                  , rs.getDouble("Lon")
          );
    }
}
