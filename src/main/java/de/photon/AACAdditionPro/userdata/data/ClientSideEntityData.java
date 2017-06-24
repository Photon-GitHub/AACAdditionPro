package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.clientsideentities.ClientsidePlayerEntity;

public class ClientSideEntityData extends TimeData
{
    public ClientsidePlayerEntity clientSidePlayerEntity;

    public ClientSideEntityData(final User theUser)
    {
        super(false, theUser);
    }
}
