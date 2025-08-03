import React from "react";
import { createBrowserRouter } from 'react-router-dom';
import ProtectedRoute from "./ProtectedRoute";
import DashboardPage from "../pages/DashboardPage"
import { RouterProvider } from 'react-router-dom'
import LoginRegisterPage from "../pages/LoginRegisterPage"

const Body = () => {

    const appRouter = createBrowserRouter([
        {
            path: "/",
            element: (
                <ProtectedRoute>
                    <DashboardPage />
                </ProtectedRoute>
            ),
        },
        {
            path: "/dashboardPage",
            element: (
                <ProtectedRoute>
                    <DashboardPage />
                </ProtectedRoute>
            ),
        },
        {
            path: "/login",
            element: <LoginRegisterPage />,
        },
    ])
    return (
        <RouterProvider router={appRouter}/>
    )
};

export default Body;