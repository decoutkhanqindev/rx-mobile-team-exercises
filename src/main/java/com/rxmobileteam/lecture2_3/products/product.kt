package com.rxmobileteam.lecture2_3.products

// loai san pham
enum class ProductCategory {
  LAPTOP,
  PHONE,
  HEADPHONES,
  SMART_WATCH,
  CAMERA,
}

// san pham
data class Product(
  val id: String,
  val name: String,
  val price: Double,
  val category: ProductCategory,
  val favoriteCount: Int,
)

// don hang
data class Order(
  val id: String,
  val products: List<Product>, // danh sach san pham
  val isDelivered: Boolean,
)

// TODO: Return a list of Product, sorted in the ascending by price. if prices are equal, sorted by favoriteCount descending
fun List<Product>.sortedByPriceAscendingThenByFavoriteCountDescending(): List<Product> =
  this.sortedWith( // sap xep tang dan cac phan tu cua collection voi comparator
    compareBy<Product> { it.price } // comparator: compare theo price cua product
      .thenByDescending { it.favoriteCount } // comparator phu: compare va sap xep des favouriteCount cua product
  )

// flatMap nhan vao 1 func lambda de chuyen doi moi phan tu cua collection thanh iterable (set, list)
// tra ve ket hop cac iterable thanh 1 collection duy nhat

// TODO: Return a set of Products in the orders (The order doesn't matter).
fun List<Order>.getProductsSet(): Set<Product> = this.flatMap { it.products }.toSet()

// TODO: Return a list of Products in the orders, duplicates are allowed.
fun List<Order>.getProductsList(): List<Product> = this.flatMap { it.products }.toList()

// TODO: Return a list of delivered orders
fun List<Order>.getDeliveredOrders(): List<Order> = this.filter { it.isDelivered == true }

// TODO: Return a list of products in the delivered orders
fun List<Order>.getDeliveredProductsList(): List<Product> = this.getDeliveredOrders().getProductsList()

// partition dung de phan chia 1 list thanh 2 list dua tren predicate
// tra ve 1 pair chua 2 list, list 1 la thoa man dieu kien <-> list 2 la khong thoa man dieu kien
// TODO: Partition the orders into two lists: "delivered" and "not delivered"
fun List<Order>.partitionDeliveredAndNotDelivered(): Pair<List<Order>, List<Order>> = this.partition { it.isDelivered }

// TODO: Return a map of product to count of this product in the orders
// eg. [Product1 -> 2, Product2 -> 1, Product3 -> 3]
fun List<Order>.countOfEachProduct(): Map<Product, Int> =
  this.getProductsList()
    // groupBy: tao ra 1 group, tra ve 1 map doi list cac group phan tu theo khoa dua tren predicate
    .groupingBy { it } // tao ra 1 group obj co the thuc hien cac phep toan tre moi obj, cac product giong nhau se chung 1 group
    .eachCount() // dem moi group obj - nhom san pham

// TODO: Return the sum of product prices in the order
fun Order.sumProductPrice(): Double = this.products.sumOf { it.price }

// TODO: Return the product with the maximum price in the order
fun Order.getMaxPriceProduct(): Product = this.products.maxBy { it.price }

// TODO: Return the product with the min price in the order
fun Order.getMinPriceProduct(): Product = this.products.minBy { it.price }

