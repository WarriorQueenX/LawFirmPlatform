
package com.lawfirm;

import com.lawfirm.dao.ClientDAO;
import com.lawfirm.model.Client;

import java.util.List;

public class Main {

    public static void main(String[] args) {

        List<Client> clients = ClientDAO.getAllClients();

        for (Client client : clients) {
            System.out.println(
                    client.getClientId() + " | " +
                            client.getFullName() + " | " +
                            client.getEmail()
            );
        }
    }
}
