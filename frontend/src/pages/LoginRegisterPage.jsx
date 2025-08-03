import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { addUser } from "../store/reducers/userSlice";

const LoginRegisterPage = () => {
    const [isSignInForm, setIsSignInForm] = useState(true);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [error, setError] = useState("");
    const navigate = useNavigate();
    const dispatch = useDispatch();
    const user = useSelector(store => store.user);


    useEffect(() => {
        if (user) {
            navigate("/dashboardPage", { replace: true });
        }
    }, [user, navigate]);

    const handleForm = () => {
        setIsSignInForm(!isSignInForm);
    };

    const handleAuth = async (e) => {
        e.preventDefault();
        setError("");
        const url = isSignInForm ? "http://localhost:8080/users/login" : "http://localhost:8080/users/register";
        try {
            const res = await fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email, password }),
            });
            if (res.ok) {
                const data = await res.json();
                console.log(data);
                dispatch(addUser({
                    id: data.id,
                    email: data.email
                }))
            } else {
                const data = await res.json().catch(() => ({}));
                setError(data.message || "Auth failed.");
            }
        } catch (err) {
            setError(err.message);
        }
    };


    return (
        <div>
            <div className="flex flex-row justify-center p-2 my-10">
                <div className="flex flex-row w-fit border-blue-300 border-b-4">
                    <div className="w-13">
                        <img src="/icons/mail.png" alt="mail" />
                    </div>
                    <div className="w-fit px-1 m-1 text-5xl font-limelight text-blue-400">
                        OneBox
                    </div>
                </div>

            </div>
            <div className="border-4 mx-100 my-15">
                <div className="m-5 text-center">
                    <h1 className="text-black text-3xl font-oswald">
                        Welcome to Onebox!
                    </h1>
                </div>
                <div className="m-10 mt-5">
                    <div className="text-center text-xl">
                        {isSignInForm ? "Login" : "Register"}
                    </div>
                    <form

                        className="border-2 mt-3 my-2"
                        autoComplete="off"
                    >
                        <div className="m-2 p-4 flex flex-col">
                            Email
                            <input
                                type="text"
                                placeholder="Enter your email address."
                                className="p-2 my-1 focus:outline-none border-black border-2"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                        <div className="m-2 -mt-8 p-4 flex flex-col">
                            Password
                            <input
                                type="password"
                                placeholder="Enter your password."
                                className="p-2 my-1 focus:outline-none border-black border-2"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                        {error && (
                            <div className="text-red-500 text-center">{error}</div>
                        )}
                        <div className="m-2">
                            <button
                                type="submit" onClick={handleAuth}
                                className="mx-4 mb-2 border-2 p-2 hover:bg-black hover:text-white">
                                {isSignInForm ? "Sign In" : "Sign Up"}
                            </button>
                        </div>
                        <div className="m-2">
                            <p className="mx-4 mb-2 cursor-pointer text-blue-500 underline" onClick={handleForm}>
                                {isSignInForm ? "New to OneBox? Register Now" : "Already have an account? Sign In Now"}
                            </p>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default LoginRegisterPage;