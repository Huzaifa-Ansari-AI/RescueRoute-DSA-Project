import { Link } from "react-router-dom";

const NotFoundPage = () => {
  return (
    <div
      style={{
        textAlign: "center",
        padding: "80px 20px",
      }}
    >
      <h1>404</h1>

      <h2>Page Not Found</h2>

      <p>
        The page you are looking for does not exist.
      </p>

      <Link to="/">
        Go Back Home
      </Link>
    </div>
  );
};

export default NotFoundPage;



// export default function NotFoundPage() {
//     return <h1>Not Found Page</h1>;
// }