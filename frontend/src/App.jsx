import React from "react"
import { Provider } from "react-redux";
import appStore from "./store/appStore";
import Body from "./components/Body";

const App = () => {

  return (
    <>
      <Provider store={appStore}>
        <Body />
      </Provider>
    </>
  )
}

export default App;