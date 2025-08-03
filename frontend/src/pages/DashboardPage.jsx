import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { removeUser } from "../store/reducers/userSlice";

const DashboardPage = () => {

    const user = useSelector(store => store.user);
    const dispatch = useDispatch();
    const navigate = useNavigate();

    const handleLogout = () => {
        dispatch(removeUser());
    };

    useEffect(() => {
        if (!user) {
            navigate("/login");
        }
    }, [user, navigate]);

    return (
        <div>
            <button className="text-amber-950 border-2" onClick={handleLogout}>Dashboard Page</button>
        </div>
    );
};

export default DashboardPage;