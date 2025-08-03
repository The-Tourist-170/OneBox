import { configureStore } from "@reduxjs/toolkit";
import userReducer from "./reducers/userSlice";

const appStore = configureStore({
    reducer: {
        user: userReducer
    }
});

export default appStore;