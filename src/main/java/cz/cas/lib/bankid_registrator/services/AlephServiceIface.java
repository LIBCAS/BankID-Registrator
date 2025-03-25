package cz.cas.lib.bankid_registrator.services;

import java.util.Map;

import cz.cas.lib.bankid_registrator.product.Connect;
import cz.cas.lib.bankid_registrator.product.Identify;

public interface AlephServiceIface extends ServiceIface
{
    public abstract Map<String, Object> newPatron(Connect userInfo, Identify userProfile);
}
