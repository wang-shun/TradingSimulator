package com.rest;

import com.rest.matching.MatchingEngine;
import com.rest.model.Orders;
import com.rest.model.common.Side;
import com.rest.socket.Server;
import com.rest.util.InstrumentManager;
import com.rest.util.OrderManager;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/order")
public class OrderNode {

    public OrderNode() {
        System.out.println("OrderNode has been instantiated now");
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@DefaultValue("1000000000") @QueryParam("limit") Integer limit) {

        try {
            List<JSONObject> orders = OrderManager.getAllOrdersJSON(limit);
            JSONArray res = new JSONArray();
            orders.stream().forEach(ord -> res.put(ord));
            return Response.status(200).entity(res.toString()).build();
        } catch (final OrderManager.OrderLookupException ex) {
            ex.printStackTrace();
        }
        return Response.status(210).entity("No orders found").build();
    }

    @POST
    @Path("/add")
    @Produces(MediaType.APPLICATION_JSON)
    public Response post(String request) {

        System.out.println("Got a POST request for ORDER: " + request);

        JSONObject req = null;
        try {
            req = new JSONObject(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Double price = Double.valueOf(req.optString("Price"));
        Double quantity = Double.valueOf(req.optString("Quantity"));

        String instCode = req.optString("InstrCode");
        String notes = req.optString("Notes");
        Side side = Side.valueOf(req.optString("Side"));

        int instID = 0;
        try {
            instID = InstrumentManager.getInstrument(instCode).getInstrument_id();
        } catch (InstrumentManager.InstrumentLookupException e) {
            e.printStackTrace();
            return Response.status(400).entity("Unknown instrument code").build();
        }

        Orders ord = new Orders(1, instID, side, price, quantity, notes);
        MatchingEngine.getInstance().addOrder(ord);

        return Response.status(200).entity(ord.getJSON().toString()).build();
    }

    @POST
    @Path("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteOrder(String request) {

        System.out.println("Got a POST request for ORDER deletion: " + request);

        JSONObject req = null;
        try {
            req = new JSONObject(request);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String orderID = req.optString("OrderID");

        Orders ord = null;
        try {
            ord = OrderManager.getOrder(orderID);
            MatchingEngine.getInstance().cancelOrder(ord);
        } catch (OrderManager.OrderLookupException e) {
            e.printStackTrace();
        }

        return Response.status(200).entity(ord.getJSON().toString()).build();
    }

}
