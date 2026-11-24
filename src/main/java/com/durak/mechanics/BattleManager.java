package com.durak.mechanics;

public class BattleManager
{
    private Status status;

    public BattleManager(Status status)
    {
        this.status = status;
    }

    public void setStatus(Status status)
    {
        this.status = status;
    }

    public Status getStatus()
    {
        return status;
    }

    @Override
    public String toString() {
        return status.toString();
    }
}
