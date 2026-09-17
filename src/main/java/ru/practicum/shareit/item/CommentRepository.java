package ru.practicum.shareit.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.model.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = "SELECT * FROM comments WHERE item_id = :itemId", nativeQuery = true)
    List<Comment> findAllByItemId(@Param("itemId") Long itemId);

    @Query(value = "SELECT * FROM comments WHERE item_id IN :itemIds", nativeQuery = true)
    List<Comment> findAllByItemIdIn(@Param("itemIds") List<Long> itemIds);
}
