export class GlobalConstatns{

    public static genericError:string = "Something went wrong. Please try again later.";

    public static unauthorized:string = "You do not have access to this page.";

    public static productExistsError:string = "Product already exists.";

    public static productAdded: string = "Product added successfully";

    public static nameRegex:string = "[a-zA-Z0-9 ]*";
    public static emailRegex:string = "[A-Za-z0-9._%-]+@[A-Za-z0-9._%-]+\\.[a-z]{2,3}";
    public static phoneNrRegex:string = "^[e0-9]{10,10}$";
    public static priceRegex:string = "[e0-9]*";

    public static error:string = "error";
}